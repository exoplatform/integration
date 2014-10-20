package org.exoplatform.forum.ext.impl;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.DataStorage;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.TransformHTML;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.portal.mop.user.UserPortal;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
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
  
  private Question question = null;

  private final static Log LOG = ExoLogger.getExoLogger(AnswerUIActivity.class);
  
  private static final String ANSWER_PAGE_NAGVIGATION = "answers";
  
  private static final String ANSWER_SPACE_NAGVIGATION = "answer";
  
  private static final String ANSWER_PORTLET_NAME = "AnswersPortlet";
  
  public AnswerUIActivity() {
  }
  
  private Question getQuestion() {
    DataStorage faqService = (DataStorage) CommonsUtils.getService(DataStorage.class);
    try {
      return faqService.getQuestionById(getQuestionId());
    } catch (Exception e) {
      LOG.debug("Failed to get question object", e);
      return null;
    }
  }
  
  protected String getViewCommentLink(ExoSocialActivity comment) {
    String itemId = comment.getTemplateParams().get(AnswersSpaceActivityPublisher.LINK_KEY);
    if (itemId == null || itemId.indexOf(Comment.COMMENT_ID) != 0) {
      itemId = "";
    }
    return new StringBuffer(getLink()).append("#").append(itemId).toString();
  }
  
  private String getNodeURL(UserNode node) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL =  ctx.createURL(NodeURL.TYPE);
    return nodeURL.setNode(node).toString();
  }
  
  private String getAnswerPortletInSpace(String spaceGroupId){
    
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    NavigationService navService = (NavigationService) container.getComponentInstance(NavigationService.class);
    NavigationContext nav = navService.loadNavigation(SiteKey.group(spaceGroupId));
    
    NodeContext<NodeContext<?>> parentNodeCtx = navService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
    
    if(parentNodeCtx.getSize() >= 1) {
      NodeContext<?> nodeCtx = parentNodeCtx.get(0);
      Collection<NodeContext<?>> children = (Collection<NodeContext<?>>) nodeCtx.getNodes();
      Iterator<NodeContext<?>> it = children.iterator();
      
      NodeContext<?> child = null;
      while(it.hasNext()) {
        child = it.next();
        if (ANSWER_SPACE_NAGVIGATION.equals(child.getName()) || child.getName().indexOf(ANSWER_PORTLET_NAME) >= 0) {
          return child.getName();
        }
      }
    }
    return StringUtils.EMPTY;
  }
  
  public String getSpaceHomeURL(String spaceGroupId) {
    if ("".equals(spaceGroupId))
      return null;
    String permanentSpaceName = spaceGroupId.split("/")[2];
    SpaceService spaceService  = (SpaceService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);
    Space space = spaceService.getSpaceByGroupId(spaceGroupId);
    //In the case, the space had been deleted then return NULL
    if (space == null) {
      return null;
    }
    
    NodeURL nodeURL =  RequestContext.getCurrentInstance().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.GROUP, SpaceUtils.SPACE_GROUP + "/"
                                        + permanentSpaceName, space.getPrettyName());
   
    return nodeURL.setResource(resource).toString(); 
  }
  
  private String getLink() {
    String[] questionPath = getQuestionId().split("/");
    String questionId = questionPath[questionPath.length - 1];
    String spaceLink = getSpaceHomeURL(getSpaceGroupId());
    if (spaceLink == null) {
      PortalRequestContext prc = Util.getPortalRequestContext();

      UserPortal userPortal = prc.getUserPortal();
      UserNavigation userNav = userPortal.getNavigation(prc.getSiteKey());
      UserNode userNode = userPortal.getNode(userNav, Scope.ALL, null, null);
      //
      UserNode answerNode = userNode.getChild(ANSWER_PAGE_NAGVIGATION);
      
      //
      if (answerNode != null) {
        String answerURI = getNodeURL(answerNode);
        return String.format("%s%s%s", answerURI, org.exoplatform.faq.service.Utils.QUESTION_ID, questionId);
      }
      return StringUtils.EMPTY;
    }
    if (getAnswerPortletInSpace(getSpaceGroupId()).length() == 0) {
      return StringUtils.EMPTY;
    }
    return String.format("%s/%s%s%s", spaceLink, getAnswerPortletInSpace(getSpaceGroupId()), org.exoplatform.faq.service.Utils.QUESTION_ID, questionId);
  }
  
  @SuppressWarnings("unused")
  private String getAnswerLink() {
    String spaceLink = getSpaceHomeURL(getSpaceGroupId());
    if (spaceLink == null) {
      return getActivityParamValue(AnswersSpaceActivityPublisher.LINK_KEY).concat(Utils.ANSWER_NOW.concat("true"));
    }
    String[] tab = getQuestionId().split("/");
    String answerLink = String.format("%s/answer%s%s", spaceLink, Utils.QUESTION_ID.concat(tab[tab.length-1]), Utils.ANSWER_NOW.concat("true"));
    return answerLink;
  }
  
  private String getNumberOfAnswers() throws Exception {
    String number_str = getActivityParamValue(AnswersSpaceActivityPublisher.NUMBER_OF_ANSWERS);

    int number = Integer.parseInt(CommonUtils.isEmpty(number_str) ? ActivityUtils.getNbOfAnswers(getQuestion()) : number_str);

    if (number == 0) {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.noAnswer");
    } else if (number == 1) {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.answer").replace("{0}", String.valueOf(number));
    } else {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.answers").replace("{0}", String.valueOf(number));
    }
  }
  
  private double getRating() {
    String rate = getActivityParamValue(AnswersSpaceActivityPublisher.QUESTION_RATING);
    if (CommonUtils.isEmpty(rate)) {
      rate = ActivityUtils.getQuestionRate(getQuestion());
    }
    try {
      return Double.parseDouble(rate);
    } catch (NumberFormatException e) {
      return 0.0;   
    }
  }
  
  private String getNumberOfComments() throws Exception {
    String number_str = getActivityParamValue(AnswersSpaceActivityPublisher.NUMBER_OF_COMMENTS);

    int number = Integer.parseInt(CommonUtils.isEmpty(number_str) ? ActivityUtils.getNbOfComments(getQuestion()) : number_str);

    if (number == 0) {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.noComment");
    } else if (number == 1) {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.comment").replace("{0}", String.valueOf(number));
    } else {
      return WebUIUtils.getLabel(null, "AnswerUIActivity.label.comments").replace("{0}", String.valueOf(number));
    }
  }
  
  protected String getQuestionTitle() {
    try {
      return getQuestion().getQuestion();
    } catch (Exception e) {
      return getActivity().getTitle();
    }
  }
  
  protected String getQuestionDetail() {
    try {
      return ActivityUtils.processContent(getQuestion().getDetail());
    } catch (Exception e) {
      return getActivity().getBody();
    }
  }
  
  private String getSpaceGroupId() {
    return ActivityUtils.getSpaceGroupId(getQuestion().getCategoryId());
  }
  
  private String getQuestionId() {
    String questionId = getActivityParamValue(AnswersSpaceActivityPublisher.QUESTION_ID);
    return CommonUtils.isEmpty(questionId) ? getActivityParamValue("QuestionId") : questionId;
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
      message = message.replace("<p>", "").replace("</p>", "\n");
      String enCodeMessage = TransformHTML.enCodeHTMLContent(message);
      DataStorage faqService = CommonsUtils.getService(DataStorage.class);
      Question question = faqService.getQuestionById(uiActivity.getQuestionId());
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
              post.setMessage(enCodeMessage);
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
              post.setMessage(enCodeMessage);
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
      
      
      ExoSocialActivity cm = uiActivity.toActivity(comment);
      ActivityManager activityM = (ActivityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ActivityManager.class);
      ExoSocialActivity activity = activityM.getActivity(faqService.getActivityIdForQuestion(question.getPath()));
      
      //Case migrate activity : if the activity is not exist or the current activity is not the same
      //as the activity associated to the question --> just add comment on this activity
      if (activity == null || ! uiActivity.getActivity().getId().equals(activity.getId())) {
        activity = uiActivity.getActivity();
        activityM.saveComment(activity, cm);
        uiActivity.refresh();
        context.addUIComponentToUpdateByAjax(uiActivity);
        uiActivity.getParent().broadcast(event, event.getExecutionPhase());
        return;
      }
      //
      comment.setComments(enCodeMessage);
      faqService.saveComment(question.getPath(), comment,  true);
      Map<String, String> templateParams = activity.getTemplateParams();
      question = uiActivity.getQuestion();
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
    }
    //
    if (!CommonUtils.isEmpty(activity.getTitle())) {
      String title = activity.getTitle().replaceAll("&amp;", "&");
      if(title.indexOf("<script") >= 0) {
        title = title.replace("<script", "&lt;script")
                     .replace("</script>", "&lt;/script&gt;");
      }
      activity.setTitle(title);
    }
    if (!CommonUtils.isEmpty(activity.getBody()) && !activity.isComment()) {
      activity.setBody(activity.getBody().replaceAll("&amp;", "&"));
    }
    return activity;
  }
  
}
