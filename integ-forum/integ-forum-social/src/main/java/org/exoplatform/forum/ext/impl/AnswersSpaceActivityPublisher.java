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
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.processor.I18NActivityUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice
 *         Lamarque</a>
 * @version $Revision$
 */
public class AnswersSpaceActivityPublisher extends AnswerEventListener {

  public static final String SPACE_APP_ID = "ks-answer:spaces";
  public static final String ANSWER_APP_ID = "answer:spaces";
  public static final String QUESTION_ID = "Id";
  public static final String LINK_KEY = "Link";
  public static final String LANGUAGE_KEY = "Language";
  public static final String QUESTION_RATING = "QuestionRating";
  public static final String NUMBER_OF_ANSWERS = "NumberOfAnswers";
  public static final String NUMBER_OF_COMMENTS = "NumberOfComments";
  
  private final static Log LOG = ExoLogger.getExoLogger(AnswersSpaceActivityPublisher.class);
  
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
    activity.setTitle(title);
    activity.setBody(body);
    activity.setType(SPACE_APP_ID);
    activity.setTemplateParams(templateParams);
    activity.setUserId(author.getId());
    return activity;
  }
  
  private Map<String, String> updateTemplateParams(Map<String, String> templateParams, String questionId, String questionRate, String nbAnswers, String nbComments, String language, String link) {
    templateParams.put(QUESTION_RATING, questionRate);
    templateParams.put(NUMBER_OF_ANSWERS, nbAnswers);
    templateParams.put(NUMBER_OF_COMMENTS, nbComments);
    templateParams.put(LINK_KEY, link);
    templateParams.put(LANGUAGE_KEY, language);
    templateParams.put(QUESTION_ID, questionId);
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
      if (activityId != null) {
        try {
          ExoSocialActivity activity = activityM.getActivity(activityId);
          StringBuilder commentTitle = new StringBuilder();
          ExoSocialActivityImpl comment = new ExoSocialActivityImpl();
          String prefix = "";
          for (PropertyChangeEvent pce : answer.getChangeEvent()) {
            commentTitle.append(prefix);
            prefix="\n";
            commentTitle.append(getAnswerMessage(pce, answer, comment));
          }
          comment.setUserId(userIdentity.getId());
          comment.setType(ANSWER_APP_ID);
          if (!commentTitle.toString().equals("")) {
            comment.setTitle(commentTitle.toString());
            activityM.saveComment(activity, comment);
          } else {
            String answerContent = formatBody(answer.getResponses());
            comment.setTitle("Answer has been submitted: "+answerContent);
            I18NActivityUtils.addResourceKey(comment, "answer-add", answerContent);
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
      String activityId = faqS.getActivityIdForQuestion(questionId);
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
      Map<String, String> templateParams = updateTemplateParams(new HashMap<String, String>(), question.getId(), getQuestionRate(question), getNbOfAnswers(question), getNbOfComments(question), question.getLanguage(), question.getLink());
      String activityId = faqS.getActivityIdForQuestion(question.getId());
      if (activityId != null) {
        try {
          ExoSocialActivity activity = activityM.getActivity(activityId);
          activity.setTitle(question.getQuestion());
          activity.setBody(formatBody(question.getDetail()));
          activity.setTemplateParams(templateParams);
          ExoSocialActivityImpl comment = new ExoSocialActivityImpl();
          comment.setType(ANSWER_APP_ID);
          comment.setUserId(userIdentity.getId());
          StringBuilder commentTitle = new StringBuilder();
          String prefix = "";
          for (PropertyChangeEvent pce : question.getChangeEvent()) {
            commentTitle.append(prefix);
            prefix="\n";
            commentTitle.append(getQuestionMessage(pce, question, comment));
          }
          activityM.updateActivity(activity);
          if (! "".equals(commentTitle.toString())) {
            comment.setTitle(commentTitle.toString());
            activityM.saveComment(activity, comment);
          }
        } catch (Exception e) {
          LOG.debug("Run in case of activity deleted and reupdate");
          activityId = null;
        }
      }
      if (activityId == null) {
        Identity streamOwner = null;
        String catId = (String) faqS.readQuestionProperty(question.getId(),FAQNodeTypes.EXO_CATEGORY_ID,String.class);
        Identity spaceIdentity = getSpaceIdentity(catId);
        if (spaceIdentity != null) {
          // publish the activity in the space stream.
          streamOwner = spaceIdentity;
        }
        List<String> categoryIds = faqS.getCategoryPath(catId);
        Collections.reverse(categoryIds);
        if (streamOwner == null) {
          streamOwner = userIdentity;
        }
        ExoSocialActivity activity = newActivity(streamOwner,question.getQuestion(),formatBody(question.getDetail()),templateParams);
        activityM.saveActivityNoReturn(streamOwner, activity);
        faqS.saveActivityIdForQuestion(question.getId(),activity.getId());
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

  private String getQuestionMessage(PropertyChangeEvent e, Question question, ExoSocialActivity comment) {
    if ("questionName".equals(e.getPropertyName())) {
      I18NActivityUtils.addResourceKey(comment, "question-update-title", question.getQuestion());
      return "Title has been updated to: "+question.getQuestion();
    } else if ("questionDetail".equals(e.getPropertyName())) {
      I18NActivityUtils.addResourceKey(comment, "question-update-detail", question.getDetail());
      return "Details has been edited to: "+formatBody(question.getDetail());
    } else if ("questionActivated".equals(e.getPropertyName())) {
      if (question.isActivated()) {
        I18NActivityUtils.addResourceKey(comment, "question-activated", null);
        return "Question has been activated.";
      } else {
        I18NActivityUtils.addResourceKey(comment, "question-unactivated", null);
        return "Question has been unactivated.";
      }
    } else if ("questionAttachment".equals(e.getPropertyName())) {
      I18NActivityUtils.addResourceKey(comment, "question-add-attachment", null);
      return "Attachment(s) has been added.";
    } else { //case of add new language
      I18NActivityUtils.addResourceKey(comment, "question-add-language", question.getLanguage());
      return "Question has been added in "+question.getLanguage();
    }
  }
  
  private String getAnswerMessage(PropertyChangeEvent e, Answer answer, ExoSocialActivity comment) {
    String answerContent = formatBody(answer.getResponses());
    if ("answerEdit".equals(e.getPropertyName())) {
      I18NActivityUtils.addResourceKey(comment, "answer-update-content", answerContent);
      return "Answer has been edited to: "+answerContent;
    } else if ("answerPromoted".equals(e.getPropertyName())) {
      I18NActivityUtils.addResourceKey(comment, "answer-promoted", answerContent);
      return "Comment "+answerContent+" has been promoted as an answer";
    } else if ("answerActivated".equals(e.getPropertyName())) {
      if (answer.getActivateAnswers()) {
        I18NActivityUtils.addResourceKey(comment, "answer-activated", answerContent);
        return "Answer has been activated: "+answerContent+".";
      } else {
        I18NActivityUtils.addResourceKey(comment, "answer-unactivated", answerContent);
        return "Answer has been unactivated: "+answerContent+".";
      }
    } else  {
      if (answer.getApprovedAnswers()) {
        I18NActivityUtils.addResourceKey(comment, "answer-approved", answerContent);
        return "Answer has been approved: "+answerContent+".";
      } else {
        I18NActivityUtils.addResourceKey(comment, "answer-disapproved", answerContent);
        return "Answer has been disapproved: "+answerContent+".";
      }
    }
  }
  
}
