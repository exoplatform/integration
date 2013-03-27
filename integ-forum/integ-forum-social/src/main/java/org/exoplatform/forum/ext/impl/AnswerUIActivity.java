package org.exoplatform.forum.ext.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.DataStorage;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.faq.service.Question;
import org.exoplatform.forum.common.TransformHTML;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.ext.activity.ForumActivityBuilder;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.processor.I18NActivityProcessor;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
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

  public AnswerUIActivity() {
  }
  
  @SuppressWarnings("unused")
  private String getViewCommentLink(ExoSocialActivity comment) {
    String questionLink = getLink();
    Map<String, String> templateParams = comment.getTemplateParams();
    String itemId = templateParams.get(AnswersSpaceActivityPublisher.LINK_KEY);
    return String.format("%s#%s", questionLink, itemId);
  }
  
  public String getSpaceHomeURL(String spaceGroupId) {
    if ("".equals(spaceGroupId))
      return null;
    String permanentSpaceName = spaceGroupId.split("/")[2];
    SpaceService spaceService  = (SpaceService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);
    Space space = spaceService.getSpaceByGroupId(spaceGroupId);
    
    NodeURL nodeURL =  RequestContext.getCurrentInstance().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.GROUP, SpaceUtils.SPACE_GROUP + "/"
                                        + permanentSpaceName, space.getPrettyName());
   
    return nodeURL.setResource(resource).toString(); 
  }
  
  private String getLink() {
    String spaceLink = getSpaceHomeURL(getSpaceGroupId());
    if (spaceLink == null) {
      return getActivityParamValue(AnswersSpaceActivityPublisher.LINK_KEY);
    }
    String[] tab = getQuestionId().split("/");
    String answerLink = String.format("%s/answer/%s", spaceLink, tab[tab.length-1]);
    return answerLink;
  }
  
  @SuppressWarnings("unused")
  private String getNumberOfAnswers() throws Exception {
    int number = Integer.parseInt(getActivityParamValue(AnswersSpaceActivityPublisher.NUMBER_OF_ANSWERS));
    if (number == 0) {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.noAnswer");
    } else if (number == 1) {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.answer").replace("{0}", String.valueOf(number));
    } else {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.answers").replace("{0}", String.valueOf(number));
    }
  }
  
  @SuppressWarnings("unused")
  private double getRating() {
    String rate = getActivityParamValue(AnswersSpaceActivityPublisher.QUESTION_RATING);
    return  Double.parseDouble(rate);
  }
  
  @SuppressWarnings("unused")
  private String getNumberOfComments() throws Exception {
    int number = Integer.parseInt(getActivityParamValue(AnswersSpaceActivityPublisher.NUMBER_OF_COMMENTS));

    if (number == 0) {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.noComment");
    } else if (number == 1) {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.comment").replace("{0}", String.valueOf(number));
    } else {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.comments").replace("{0}", String.valueOf(number));
    }
  }
  
  private String getSpaceGroupId() {
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
      OrganizationService organizationService = (OrganizationService) ExoContainerContext.getCurrentContainer()
                                                                                         .getComponentInstanceOfType(OrganizationService.class);
      User user = organizationService.getUserHandler().findUserByName(userName);
      String fullName = user.getFullName();
      if (fullName == null || fullName.trim().length() <= 0)
        fullName = userName;
      return fullName;
    } catch (Exception e) { // UserHandler
      return userName;
    }
  }

  public static String getLinkDiscuss(String topicId) throws Exception {
    PortalRequestContext portalContext = Util.getPortalRequestContext();
    String link = portalContext.getRequest().getRequestURL().toString();
    String selectedNode = Util.getUIPortal().getSelectedUserNode().getURI();
    String portalName = "/" + Util.getUIPortal().getName();
    if (link.indexOf(portalName) > 0) {
      if (link.indexOf(portalName + "/" + selectedNode) < 0) {
        link = link.replaceFirst(portalName, portalName + "/" + selectedNode);
      }
    }
    link = link.substring(0, link.indexOf(selectedNode) + selectedNode.length());
    link = link.replaceAll(selectedNode, "forum") + "/" + org.exoplatform.forum.service.Utils.TOPIC
        + "/" + topicId;
    return link;
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
      DataStorage faqService = (DataStorage) ExoContainerContext.getCurrentContainer()
                                                              .getComponentInstanceOfType(DataStorage.class);
      Question question = faqService.getQuestionById(uiActivity.getActivityParamValue(AnswersSpaceActivityPublisher.QUESTION_ID));
      Comment comment = new Comment();
      comment.setNew(true);
      comment.setCommentBy(context.getRemoteUser());
      comment.setComments(message);
      comment.setFullName(getFullName(context.getRemoteUser()));
      comment.setDateComment(new Date());
      // add new corresponding post to forum.
      String topicId = question.getTopicIdDiscuss();
      if (topicId != null) {
        if (topicId.length() > 0) {
          ForumService forumService = (ForumService) ExoContainerContext.getCurrentContainer()
                                                                        .getComponentInstanceOfType(ForumService.class);
          Topic topic = (Topic) forumService.getObjectNameById(topicId,
                                                               org.exoplatform.forum.service.Utils.TOPIC);
          if (topic != null) {
            String remoteAddr = WebUIUtils.getRemoteIP();
            String[] ids = topic.getPath().split("/");
            int t = ids.length;
            String linkForum = getLinkDiscuss(topicId);
            String postId = comment.getPostId();
            if (postId == null || postId.length() == 0) {
              Post post = new Post();
              post.setOwner(context.getRemoteUser());
              post.setIcon("ViewIcon");
              post.setName("Re: " + question.getQuestion());
              post.setMessage(comment.getComments());
              post.setLink(linkForum);
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
                post.setLink(linkForum);
                post.setRemoteAddr(remoteAddr);
              } else {
                post.setModifiedBy(context.getRemoteUser());
              }
              post.setIsApproved(!topic.getIsModeratePost());
              post.setMessage(comment.getComments());
              forumService.savePost(ids[t - 3],
                                    ids[t - 2],
                                    topicId,
                                    post,
                                    isNew,
                                    new MessageBuilder());
            }
  
          }
        }
      } // end adding post to forum.

      faqService.saveComment(question.getPath(), comment,  true);
      ExoSocialActivity cm = uiActivity.toActivity(comment);
      ActivityManager activityM = (ActivityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ActivityManager.class);
      ExoSocialActivity activity = activityM.getActivity(faqService.getActivityIdForQuestion(question.getPath()));
      Map<String, String> templateParams = activity.getTemplateParams();
      question = faqService.getQuestionById(uiActivity.getActivityParamValue(AnswersSpaceActivityPublisher.QUESTION_ID));
      templateParams.put(AnswersSpaceActivityPublisher.NUMBER_OF_COMMENTS, String.valueOf(question.getComments().length));
      activity.setTemplateParams(templateParams);
      activityM.updateActivity(activity);
      
      Map<String, String> commentTemplateParams = new HashMap<String, String>();
      commentTemplateParams.put(AnswersSpaceActivityPublisher.LINK_KEY, question.getLink());
      cm.setTemplateParams(commentTemplateParams);
      activityM.saveComment(activity, cm);
      faqService.saveActivityIdForComment(question.getPath(), comment.getId(), question.getLanguage(), cm.getId());
      uiActivity.refresh();
      context.addUIComponentToUpdateByAjax(uiActivity);

      uiActivity.getParent().broadcast(event, event.getExecutionPhase());
    }

  }
  
  private static String getNbOfComments(Question question) {
    int numberOfComments = (question.getComments() != null) ? question.getComments().length : 0;
    return String.valueOf(numberOfComments);
  }
  
  @Override
  protected ExoSocialActivity getI18N(ExoSocialActivity activity) {
    WebuiRequestContext requestContext = WebuiRequestContext.getCurrentInstance();
    I18NActivityProcessor i18NActivityProcessor = getApplicationComponent(I18NActivityProcessor.class);
    if (activity.getTitleId() != null) {
      Locale userLocale = requestContext.getLocale();
      activity = i18NActivityProcessor.processKeys(activity, userLocale);
    }
    return activity;
  }
  
}
