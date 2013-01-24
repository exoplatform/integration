/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.ext.impl;

import java.beans.PropertyChangeEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.faq.service.Answer;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.FAQNodeTypes;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.faq.service.impl.AnswerEventListener;
import org.exoplatform.forum.common.TransformHTML;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.BaseActivityProcessorPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice
 *         Lamarque</a>
 * @version $Revision$
 */
public class AnswersSpaceActivityPublisher extends AnswerEventListener {

  public static final String SPACE_APP_ID = "ks-answer:spaces";
  
  public static final String QUESTION_ID_KEY = "QuestionId";
  public static final String ANSWER_ID_KEY = "AnswerId";
  public static final String COMMENT_ID_KEY = "CommentId";
  public static final String ACTIVITY_TYPE_KEY = "ActivityType";
  public static final String AUTHOR_KEY = "Author";
  public static final String LINK_KEY = "Link";
  public static final String QUESTION_NAME_KEY = "Name";
  public static final String LANGUAGE_KEY = "Language";
  public static final String ANSWER = "Answer";
  public static final String QUESTION = "Question";
  public static final String COMMENT = "Comment";
  public static final String ANSWER_ADD = ANSWER + "Add";
  public static final String QUESTION_ADD = QUESTION + "Add";
  public static final String COMMENT_ADD = COMMENT + "Add";
  public static final String ANSWER_UPDATE = ANSWER + "Update";
  public static final String QUESTION_UPDATE = QUESTION + "Update";
  public static final String COMMENT_UPDATE = COMMENT + "Update";
  public static final String ICON = "ActivityIcon";
  public static final String ANSWER_ACTIVITY_TYPE = "AnswerActivityType";
  public static final String QUESTION_RATING = "QuestionRating";
  public static final String NUMBER_OF_ANSWERS = "NumberOfAnswers";
  public static final String NUMBER_OF_COMMENTS = "NumberOfComments";
  
  private final static Log LOG = ExoLogger.getExoLogger(AnswersSpaceActivityPublisher.class);
  
  private boolean isCategoryPublic(String categoryId, List<String> categories) throws Exception {
    if (categoryId != null) {
      FAQService faqS = (FAQService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(FAQService.class);
      String[] users = (String[]) faqS.readCategoryProperty(categoryId, FAQNodeTypes.EXO_USER_PRIVATE, String[].class);
      int parentIndex = categories.indexOf(categoryId) - 1; 
        
      return org.exoplatform.forum.service.Utils.isEmpty(users) && (parentIndex < 0 ? true : isCategoryPublic(categories.get(parentIndex), categories));
    }
    return false;
  }
  
  private boolean isQuestionPublic(Question question) {
    // the question is public if it is not activated or approved
    return question != null && question.isActivated() && question.isApproved();
  }
  
  private boolean isAnswerPublic(Answer answer) {
    // the answer is public if it is not activated or approved
    return answer != null && answer.getApprovedAnswers() && answer.getActivateAnswers();
  }
  
  private Identity getSpaceIdentity(String categoryId) {
    if (categoryId.indexOf(Utils.CATE_SPACE_ID_PREFIX) < 0) 
      return null;
    String prettyname = categoryId.split(Utils.CATE_SPACE_ID_PREFIX)[1];
    IdentityManager identityM = (IdentityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(IdentityManager.class);
    SpaceService spaceService  = (SpaceService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);
    Space space = spaceService.getSpaceByPrettyName(prettyname);
    if (space != null)
      return identityM.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
    else return null;
  }
  
  private ExoSocialActivity newActivity(Identity author, String title, String body, Map<String, String> templateParams) {
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setUserId(author.getId());
    //activity.setTitle(StringEscapeUtils.unescapeHtml(title));
    //activity.setBody(StringEscapeUtils.unescapeHtml(TransformHTML.cleanHtmlCode(body, (List<String>) Collections.EMPTY_LIST)));
    activity.setTitle(title);
    activity.setBody(body);
    activity.setType(SPACE_APP_ID);
    activity.setTemplateParams(templateParams);
    return activity;
  }
  
  private Map<String, String> updateTemplateParams(Map<String, String> templateParams, String questionId, String questionRate, String nbAnswers, String nbComments, String language, String link) {
    templateParams.put(QUESTION_RATING, questionRate);
    templateParams.put(NUMBER_OF_ANSWERS, nbAnswers);
    templateParams.put(NUMBER_OF_COMMENTS, nbComments);
    templateParams.put(LINK_KEY, link);
    templateParams.put(LANGUAGE_KEY, language);
    templateParams.put(QUESTION_ID_KEY, questionId);
    return templateParams;
  }
  
  @Override
  public void saveAnswer(String questionId, Answer answer, boolean isNew) {
    try {
      ExoContainer exoContainer = ExoContainerContext.getCurrentContainer();
      IdentityManager identityM = (IdentityManager) exoContainer.getComponentInstanceOfType(IdentityManager.class);
      ActivityManager activityM = (ActivityManager) exoContainer.getComponentInstanceOfType(ActivityManager.class);
      FAQService faqS = (FAQService) exoContainer.getComponentInstanceOfType(FAQService.class);
      Question question = faqS.getQuestionById(questionId);
      Identity userIdentity = identityM.getOrCreateIdentity(OrganizationIdentityProvider.NAME,answer.getResponseBy(),false);
      String activityId = faqS.getActivityIdForQuestion(questionId);
      Map<String, String> templateParams = new HashMap<String, String>();
      if (activityId != null) {
        try {
          ExoSocialActivity activity = activityM.getActivity(activityId);
          StringBuilder commentTitle = new StringBuilder();
          String prefix = "";
          for (PropertyChangeEvent pce : answer.getChangeEvent()) {
            commentTitle.append(prefix);
            prefix="\n";
            commentTitle.append(getAnswerMessage(pce, answer));
          }
          ExoSocialActivityImpl comment = new ExoSocialActivityImpl();
          comment.setUserId(userIdentity.getId());
          comment.setTitleId(ANSWER_ID_KEY);
          comment.setType(ANSWER_ACTIVITY_TYPE);
          templateParams.put(ANSWER, answer.getResponses());
          templateParams.put(BaseActivityProcessorPlugin.TEMPLATE_PARAM_TO_PROCESS, ANSWER);
          comment.setTemplateParams(templateParams);
          if (!commentTitle.toString().equals("")) {
            comment.setTitle(commentTitle.toString());
            activityM.saveComment(activity, comment);
          } else {
            comment.setTitle("Answer has been submitted: "+formatBody(answer.getResponses()));
            Map<String, String> activityTemplateParams = updateTemplateParams(new HashMap<String, String>(),questionId, getQuestionRate(question), getNbOfAnswers(question), getNbOfComments(question), question.getLanguage(), question.getLink());
            activity.setTemplateParams(activityTemplateParams);
            activity.setBody(formatBody(question.getDetail()));
            activityM.updateActivity(activity);
            activityM.saveComment(activity, comment);
            faqS.saveActivityIdForAnswer(questionId, answer, comment.getId());
          }
          
        } catch (Exception e) {
          LOG.debug("Run in case of activity deleted and reupdate");
          activityId = null;
        }
      }
      if (activityId == null) {
        saveQuestion(question, false);
      }
    } catch (Exception e) { // FQAService
      LOG.error("Can not record Activity for space when post answer ", e);
    }
  }

  @Override
  public void saveComment(String questionId, Comment cm, String language) {
    try {
      ExoContainer exoContainer = ExoContainerContext.getCurrentContainer();
      IdentityManager identityM = (IdentityManager) exoContainer.getComponentInstanceOfType(IdentityManager.class);
      ActivityManager activityM = (ActivityManager) exoContainer.getComponentInstanceOfType(ActivityManager.class);
      FAQService faqS = (FAQService) exoContainer.getComponentInstanceOfType(FAQService.class);
      Question question = faqS.getQuestionById(questionId);
        Identity userIdentity = identityM.getOrCreateIdentity(OrganizationIdentityProvider.NAME, cm.getCommentBy(), false);
        //String questionPath = question.getCategoryPath()+"/"+question.getId();
        String activityId = faqS.getActivityIdForQuestion(questionId);
        Map<String, String> templateParams = new HashMap<String, String>();
        if (activityId != null) {
          try {
            ExoSocialActivity activity = activityM.getActivity(activityId);
            ExoSocialActivityImpl comment = new ExoSocialActivityImpl();
            String commentActivityId = faqS.getActivityIdForComment(questionId, cm.getId(), language);
            if (commentActivityId != null) {
              ExoSocialActivityImpl oldComment = (ExoSocialActivityImpl) activityM.getActivity(commentActivityId);
              if (oldComment != null) {
                comment = oldComment;
                comment.setTitle(cm.getComments());
                activityM.updateActivity(comment);
              } else {
                commentActivityId = null;
              }
            }
            if (commentActivityId == null) {
              comment.setTitle(cm.getComments());
              comment.setUserId(userIdentity.getId());
              comment.setTitleId(COMMENT_ID_KEY);
              comment.setType(ANSWER_ACTIVITY_TYPE);
              //templateParams.put(ANSWER, answer.getResponses());
              //templateParams.put(BaseActivityProcessorPlugin.TEMPLATE_PARAM_TO_PROCESS, ANSWER);
              comment.setTemplateParams(templateParams);
              Map<String, String> activityTemplateParams = updateTemplateParams(new HashMap<String, String>(), questionId, getQuestionRate(question), getNbOfAnswers(question), getNbOfComments(question), question.getLanguage(), question.getLink());
              activity.setTemplateParams(activityTemplateParams);
              activity.setBody(formatBody(question.getDetail()));
              activityM.updateActivity(activity);
              activityM.saveComment(activity, comment);
              faqS.saveActivityIdForComment(questionId, cm.getId(), language, comment.getId());
            }
          } catch (Exception e) {
            LOG.debug("Run in case of activity deleted and reupdate");
            activityId = null;
          }
        } 
        if (activityId == null) {
          saveQuestion(question, false);
        }
    } catch (Exception e) { //FQAService      
      LOG.error("Can not record Activity for space when post comment ", e);
    }

  }

  @Override
  public void saveQuestion(Question question, boolean isNew) {
    try {
      ExoContainer exoContainer = ExoContainerContext.getCurrentContainer();
      IdentityManager identityM = (IdentityManager) exoContainer.getComponentInstanceOfType(IdentityManager.class);
      ActivityManager activityM = (ActivityManager) exoContainer.getComponentInstanceOfType(ActivityManager.class);
      FAQService faqS = (FAQService) exoContainer.getComponentInstanceOfType(FAQService.class);
      Identity userIdentity = identityM.getOrCreateIdentity(OrganizationIdentityProvider.NAME,question.getAuthor(),false);
      Identity streamOwner = null, author = userIdentity;
      String catId = (String) faqS.readQuestionProperty(question.getId(),FAQNodeTypes.EXO_CATEGORY_ID,String.class);
      Identity spaceIdentity = getSpaceIdentity(catId);
      if (spaceIdentity != null) {
        // publish the activity in the space stream.
        streamOwner = spaceIdentity;
      }
      List<String> categoryIds = faqS.getCategoryPath(catId);
      Collections.reverse(categoryIds);
      if (streamOwner == null && isCategoryPublic(catId, categoryIds)) {
        streamOwner = userIdentity;
      }
      if (streamOwner != null) {
        Map<String, String> templateParams = updateTemplateParams(new HashMap<String, String>(), question.getId(), getQuestionRate(question), getNbOfAnswers(question), getNbOfComments(question), question.getLanguage(), question.getLink());
        ExoSocialActivity activity = newActivity(author,question.getQuestion(),formatBody(question.getDetail()),templateParams);
        String activityId = faqS.getActivityIdForQuestion(question.getCategoryPath() + "/" + question.getId());
        if (activityId != null) {
          ExoSocialActivity got = activityM.getActivity(activityId);
          if (got != null) {
            activity = got;
            activity.setTitle(question.getQuestion());
            activity.setBody(formatBody(question.getDetail()));
            Map<String, String> activityTemplateParams = updateTemplateParams(new HashMap<String, String>(), question.getId(), getQuestionRate(question), getNbOfAnswers(question), getNbOfComments(question), question.getLanguage(), question.getLink());
            activity.setTemplateParams(activityTemplateParams);
            ExoSocialActivityImpl comment = new ExoSocialActivityImpl();
            comment.setUserId(userIdentity.getId());
            StringBuilder commentTitle = new StringBuilder();
            String prefix = "";
            for (PropertyChangeEvent pce : question.getChangeEvent()) {
              commentTitle.append(prefix);
              prefix="\n";
              commentTitle.append(getQuestionMessage(pce, question));
            }
            if (!commentTitle.toString().equals("")) {
              comment.setTitle(commentTitle.toString());
              activityM.saveComment(activity, comment);
            }
            activityM.updateActivity(activity);
          } else {
            activityId = null;
          }
        }
        if (activityId == null) {
          activityM.saveActivityNoReturn(streamOwner, activity);
          faqS.saveActivityIdForQuestion(question.getCategoryPath() + "/" + question.getId(),activity.getId());
        }
      }
    } catch (Exception e) { // FQAService
      LOG.error("Can not record Activity for space when add new question ", e);
    }
  }

  @Override
  public void saveAnswer(String questionId, Answer[] answers, boolean isNew) {
    try {
      Class.forName("org.exoplatform.social.core.manager.IdentityManager");
      if (answers != null) {
        for (Answer a : answers) {
          saveAnswer(questionId, a, isNew);
        }
      }
      
    } catch (ClassNotFoundException e) {
      if (LOG.isDebugEnabled())
        LOG.debug("Please check the integrated project does the social deploy? " + e.getMessage());
    } 
    //catch other type of exception in saveAnswer(String questionId, Answer answers, boolean isNew)
  }
  
  public void voteQuestion(String questionId) {
    try {
      ExoContainer exoContainer = ExoContainerContext.getCurrentContainer();
      FAQService faqS = (FAQService) exoContainer.getComponentInstanceOfType(FAQService.class);
      Question question = faqS.getQuestionById(questionId);
      
      //No event is created because old and new values are equal but we must update the activity's content
      question.setEditedQuestionRating(question.getMarkVote());
      saveQuestion(question, false);
    } catch (Exception e) {
      LOG.debug("Fail to vote question "+e.getMessage());
    }
  }
  
  public void unVoteQuestion(String questionId) {
    try {
      ExoContainer exoContainer = ExoContainerContext.getCurrentContainer();
      FAQService faqS = (FAQService) exoContainer.getComponentInstanceOfType(FAQService.class);
      Question question = faqS.getQuestionById(questionId);
      
      //No event is created because old and new values are equal but we must update the activity's content
      question.setEditedQuestionRating(question.getMarkVote());
      saveQuestion(question, false);
    } catch (Exception e) {
      LOG.debug("Fail to unvote question "+e.getMessage());
    }
  }
  
  public void removeQuestion(String questionActivityId) {
    try {
      ExoContainer exoContainer = ExoContainerContext.getCurrentContainer();
      ActivityManager activityM = (ActivityManager) exoContainer.getComponentInstanceOfType(ActivityManager.class);
      ExoSocialActivity activity = activityM.getActivity(questionActivityId);
      activityM.deleteActivity(activity);
    } catch (Exception e) {
      LOG.debug("Fail to remove activity when remove question "+e.getMessage());
    }
  }
  
  public void removeAnswer(String questionPath, String answerId) {
    try {
      ExoContainer exoContainer = ExoContainerContext.getCurrentContainer();
      FAQService faqS = (FAQService) exoContainer.getComponentInstanceOfType(FAQService.class);
      Answer answer = faqS.getAnswerById(questionPath, answerId);
      String answerActivityId = faqS.getActivityIdForAnswer(questionPath, answer);
      String questionActivityId = faqS.getActivityIdForQuestion(questionPath);
      ActivityManager activityM = (ActivityManager) exoContainer.getComponentInstanceOfType(ActivityManager.class);
      activityM.deleteComment(questionActivityId, answerActivityId);
    } catch (Exception e) {
      LOG.debug("Fail to remove comment when remove question's answer "+e.getMessage());
    }
  }
  
  public void removeComment(String questionActivityId, String commentActivityId) {
    try {
      ExoContainer exoContainer = ExoContainerContext.getCurrentContainer();
      ActivityManager activityM = (ActivityManager) exoContainer.getComponentInstanceOfType(ActivityManager.class);
      activityM.deleteComment(questionActivityId, commentActivityId);
    } catch (Exception e) {
      LOG.debug("Fail to remove comment when remove question's comment "+e.getMessage());
    }
  }
  
  private String formatBody(String body) {
    String[] tab = body.split("\\r?\\n");
    int length = tab.length;
    if (length > 4) length = 4;
    StringBuilder sb = new StringBuilder();
    String prefix = "";
    for (int i=0; i<length; i++) {
      sb.append(prefix);
      prefix = "\n";
      sb.append(StringEscapeUtils.unescapeHtml(TransformHTML.cleanHtmlCode(tab[i], (List<String>) Collections.EMPTY_LIST)));
    }
    return sb.toString();
  }
  
  private String getQuestionRate(Question question) {
    return String.valueOf(question.getMarkVote());
  }
  
  private String getNbOfAnswers(Question question) {
    int numberOfAnswers = (question.getAnswers() != null) ? question.getAnswers().length : 0;
    return String.valueOf(numberOfAnswers);
  }
  
  private String getNbOfComments(Question question) {
    int numberOfAnswers = (question.getAnswers() != null) ? question.getAnswers().length : 0;
    int numberOfComments = (question.getComments() != null) ? question.getComments().length + numberOfAnswers : 0+numberOfAnswers;
    return String.valueOf(numberOfComments);
  }

  private String getQuestionMessage(PropertyChangeEvent e, Question question) {
    if ("questionName".equals(e.getPropertyName())) {
      return "Title has been updated to: "+question.getQuestion();
    } else if ("questionDetail".equals(e.getPropertyName())) {
      return "Details has been edited to: "+formatBody(question.getDetail());
    } else if ("questionActivated".equals(e.getPropertyName())) {
      return (question.isActivated()) ? "Question has been activated." : "Question has been unactivated.";
    } else if ("questionAttachment".equals(e.getPropertyName())) {
      return "Attachment(s) has been added.";
    } else { //case of add new language
      return "Question has been added in "+question.getLanguage();
    }
  }
  
  private String getAnswerMessage(PropertyChangeEvent e, Answer answer) {
    String answerContent = formatBody(answer.getResponses());
    if (e.getPropertyName().equals("answerEdit")) {
      return "Answer has been edited to: "+answerContent;
    } else if (e.getPropertyName().equals("answerActivated")) {
      return (answer.getActivateAnswers()) ? "Answer has been activated: "+answerContent+"." : "Answer has been unactivated: "+answerContent+".";
    } else  {
      return (answer.getApprovedAnswers()) ? "Answer has been approved: "+answerContent+"." : "Answer has been disapproved: "+answerContent+".";
    }
  }
  
}
