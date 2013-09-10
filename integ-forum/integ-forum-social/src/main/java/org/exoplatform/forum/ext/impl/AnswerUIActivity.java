package org.exoplatform.forum.ext.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.DataStorage;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.TransformHTML;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.ext.activity.BuildLinkUtils;
import org.exoplatform.forum.ext.activity.BuildLinkUtils.PORTLET_INFO;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.processor.I18NActivityProcessor;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.form.UIFormTextAreaInput;

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/forum/social-integration/plugin/space/AnswerUIActivity.gtmpl", events = {
    @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
    @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
    @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
    @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
    @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
    @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class),
    @EventConfig(listeners = AnswerUIActivity.PostCommentActionListener.class) })
public class AnswerUIActivity extends BaseKSActivity {
  
  private Question question = null;

  public AnswerUIActivity() {
  }
  
  private Question getQuestion() throws Exception {
    String questionId = getQuestionId();
    if (question == null || questionId.indexOf(question.getId()) < 0) {
      question = CommonsUtils.getService(FAQService.class).getQuestionById(questionId);
    }
    return question;
  }
  
  protected String getViewCommentLink(ExoSocialActivity comment) {
    String itemId = comment.getTemplateParams().get(AnswersSpaceActivityPublisher.LINK_KEY);
    if (itemId == null || itemId.indexOf(Comment.COMMENT_ID) != 0) {
      itemId = "";
    }
    return new StringBuffer(getLink()).append("#").append(itemId).toString();
  }

  protected String getLink() {
    try {
      return BuildLinkUtils.buildLink(getQuestion().getCategoryId(), getQuestion().getId(), PORTLET_INFO.ANSWER);
    } catch (Exception e) {
      return "";
    }
  }
  
  protected String getAnswerLink() {
    return new StringBuffer(getLink()).append(Utils.ANSWER_NOW).append("true").toString();
  }
  
  protected String getNumberOfAnswers() throws Exception {
    int number = Integer.parseInt(getActivityParamValue(AnswersSpaceActivityPublisher.NUMBER_OF_ANSWERS));
    if (number == 0) {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.noAnswer");
    } else if (number == 1) {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.answer").replace("{0}", String.valueOf(number));
    } else {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.answers").replace("{0}", String.valueOf(number));
    }
  }
  
  protected double getRating() {
    String rate = getActivityParamValue(AnswersSpaceActivityPublisher.QUESTION_RATING);
    return  Double.parseDouble(rate);
  }
  
  protected String getNumberOfComments() throws Exception {
    int number = Integer.parseInt(getActivityParamValue(AnswersSpaceActivityPublisher.NUMBER_OF_COMMENTS));

    if (number == 0) {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.noComment");
    } else if (number == 1) {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.comment").replace("{0}", String.valueOf(number));
    } else {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.comments").replace("{0}", String.valueOf(number));
    }
  }
  
  protected String getSpaceGroupId() {
    return getActivityParamValue(AnswersSpaceActivityPublisher.SPACE_GROUP_ID);
  }
  
  private String getQuestionId() {
    return getActivityParamValue(AnswersSpaceActivityPublisher.QUESTION_ID);
  }

  private ExoSocialActivity toActivity(Comment comment) {
    ExoSocialActivity activity = null;
    if (comment != null) {
      activity = new ExoSocialActivityImpl();
      IdentityManager identityM = (IdentityManager) ExoContainerContext.getCurrentContainer()
                                                                       .getComponentInstanceOfType(IdentityManager.class);
      Identity userIdentity = identityM.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                            comment.getCommentBy(),
                                                            false);
      activity.setUserId(userIdentity.getId());
      activity.setTitle(comment.getComments());
      activity.setPostedTime(comment.getDateComment().getTime());
      activity.setId(comment.getId());

    }
    return activity;
  }

  static public String getFullName(String userName) throws Exception {
    try {
      OrganizationService organizationService = CommonsUtils.getService(OrganizationService.class);
      User user = organizationService.getUserHandler().findUserByName(userName);
      String fullName = user.getDisplayName();
      if (fullName == null || fullName.trim().length() <= 0) {
        fullName = new StringBuffer(user.getFirstName()).append(" ").append(user.getLastName()).toString();
      }
      return fullName;
    } catch (Exception e) {
      return userName;
    }
  }

  public int getQuestionPoint() {

    try {

      String sQuestionPoint = getActivityParamValue(AnswersSpaceActivityPublisher.QUESTION_POINT);
      
      if (CommonUtils.isEmpty(sQuestionPoint)) {// If the QUESTION_POINT is not exist in Template Params  
        //
        DataStorage faqService = CommonsUtils.getService(DataStorage.class);
        int questionPoint = Utils.getQuestionPoint(getQuestion());
        
        //update again the activity
        ActivityManager activityM = CommonsUtils.getService(ActivityManager.class);
        ExoSocialActivity activity = activityM.getActivity(faqService.getActivityIdForQuestion(question.getPath()));
        
        Map<String, String> templateParams = activity.getTemplateParams();
        templateParams.put(AnswersSpaceActivityPublisher.QUESTION_POINT, String.valueOf(questionPoint));
        activityM.updateActivity(activity);
        
        return questionPoint;
      }

      return Integer.parseInt(sQuestionPoint);
    } catch (Exception e) {
      return 0;
    }

  }

  public static class PostCommentActionListener extends BaseUIActivity.PostCommentActionListener {

    @Override
    public void execute(Event<BaseUIActivity> event) throws Exception {
      AnswerUIActivity uiActivity = (AnswerUIActivity) event.getSource();
      WebuiRequestContext context = event.getRequestContext();
      UIFormTextAreaInput uiFormComment = uiActivity.getChild(UIFormTextAreaInput.class);
      String message = uiFormComment.getValue();
      if (message == null || message.trim().length() == 0) {
        context.getUIApplication()
               .addMessage(new ApplicationMessage("AnswerUIActivity.msg.content-empty",
                                                  null,
                                                  ApplicationMessage.WARNING));
        return;
      }

      Question question = uiActivity.getQuestion();
      Comment comment = new Comment();
      comment.setNew(true);
      comment.setCommentBy(context.getRemoteUser());
      comment.setComments(TransformHTML.enCodeHTMLContent(message));
      comment.setFullName(getFullName(context.getRemoteUser()));
      comment.setDateComment(new Date());
      // add new corresponding post to forum.
      String topicId = question.getTopicIdDiscuss();
      if (topicId != null) {
        if (topicId.length() > 0) {
          ForumService forumService = CommonsUtils.getService(ForumService.class);
          Topic topic = (Topic) forumService.getObjectNameById(topicId,
                                                               org.exoplatform.forum.service.Utils.TOPIC);
          if (topic != null) {
            String remoteAddr = WebUIUtils.getRemoteIP();
            String[] ids = topic.getPath().split("/");
            int t = ids.length;
            String postId = comment.getPostId();
            if (postId == null || postId.length() == 0) {
              Post post = new Post();
              post.setOwner(context.getRemoteUser());
              post.setIcon("ViewIcon");
              post.setName("Re: " + question.getQuestion());
              post.setMessage(comment.getComments());
              post.setLink(postId);
              post.setIsApproved(!topic.getIsModeratePost());
              post.setRemoteAddr(remoteAddr);
              forumService.savePost(ids[t - 3], ids[t - 2], topicId, post, true, new MessageBuilder());
              comment.setPostId(post.getId());
            } else {
              Post post = forumService.getPost(ids[t - 3], ids[t - 2], topicId, postId);
              boolean isNew = false;
              if (post == null) {
                post = new Post();
                isNew = true;
                post.setOwner(context.getRemoteUser());
                post.setIcon("ViewIcon");
                post.setName("Re: " + question.getQuestion());
                comment.setPostId(post.getId());
                post.setLink(postId);
                post.setRemoteAddr(remoteAddr);
              } else {
                post.setModifiedBy(context.getRemoteUser());
              }
              post.setIsApproved(!topic.getIsModeratePost());
              post.setMessage(comment.getComments());
              forumService.savePost(ids[t - 3], ids[t - 2], topicId, post, isNew, new MessageBuilder());
            }
  
          }
        }
      } // end adding post to forum.
      
      DataStorage faqService = CommonsUtils.getService(DataStorage.class);
      faqService.saveComment(question.getPath(), comment,  true);
      comment.setComments(message);
      ExoSocialActivity cm = uiActivity.toActivity(comment);
      ActivityManager activityM = (ActivityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ActivityManager.class);
      ExoSocialActivity activity = activityM.getActivity(faqService.getActivityIdForQuestion(question.getPath()));
      Map<String, String> templateParams = activity.getTemplateParams();
      question = faqService.getQuestionById(uiActivity.getActivityParamValue(AnswersSpaceActivityPublisher.QUESTION_ID));
      templateParams.put(AnswersSpaceActivityPublisher.NUMBER_OF_COMMENTS, String.valueOf(question.getComments().length));
      activity.setTemplateParams(templateParams);
      activity.setBody(null);
      activity.setTitle(null);
      activityM.updateActivity(activity);
      
      Map<String, String> commentTemplateParams = new HashMap<String, String>();
      commentTemplateParams.put(AnswersSpaceActivityPublisher.LINK_KEY, comment.getId());
      cm.setTemplateParams(commentTemplateParams);
      activityM.saveComment(activity, cm);
      faqService.saveActivityIdForComment(question.getPath(), comment.getId(), question.getLanguage(), cm.getId());
      uiActivity.refresh();
      context.addUIComponentToUpdateByAjax(uiActivity);

      uiActivity.getParent().broadcast(event, event.getExecutionPhase());
    }

  }
  
  @Override
  protected ExoSocialActivity getI18N(ExoSocialActivity activity) {
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    I18NActivityProcessor i18NActivityProcessor = getApplicationComponent(I18NActivityProcessor.class);
    if (activity.getTitleId() != null) {
      Locale userLocale = requestContext.getLocale();
      activity = i18NActivityProcessor.processKeys(activity, userLocale);
      String title = activity.getTitle().replaceAll("&amp;", "&");
      activity.setTitle(title);
      if (activity.isComment() == false) {
        String body = activity.getBody().replaceAll("&amp;", "&");
        activity.setBody(body);
      }
    }
    return activity;
  }
  
}
