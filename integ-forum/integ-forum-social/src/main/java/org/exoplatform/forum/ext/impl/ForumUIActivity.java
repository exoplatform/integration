package org.exoplatform.forum.ext.impl;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.TransformHTML;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.ext.activity.BuildLinkUtils;
import org.exoplatform.forum.ext.activity.BuildLinkUtils.PORTLET_INFO;
import org.exoplatform.forum.ext.activity.ForumActivityBuilder;
import org.exoplatform.forum.ext.activity.ForumActivityContext;
import org.exoplatform.forum.ext.activity.ForumActivityUtils;
import org.exoplatform.forum.service.DataStorage;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.processor.I18NActivityProcessor;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.lifecycle.WebuiBindingContext;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.form.UIFormTextAreaInput;

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/forum/social-integration/plugin/space/ForumUIActivity.gtmpl", events = {
    @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
    @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
    @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
    @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
    @EventConfig(listeners = ForumUIActivity.PostCommentActionListener.class),
    @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
    @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class) })
public class ForumUIActivity extends BaseKSActivity {

  private static final Log LOG = ExoLogger.getLogger(ForumUIActivity.class);
  
  private static final String SPACE_GROUP_ID  = "SpaceGroupId";

  public ForumUIActivity() {
    
  }

  protected String getReplyLink() {
    String viewLink = buildLink();
    
    StringBuffer sb = new StringBuffer(viewLink);
    if (sb.lastIndexOf("/") == -1 || sb.lastIndexOf("/") != sb.length() - 1) {
      sb.append("/");
    }
    // add signal to show reply form
    sb.append("lastpost/false");
    return sb.toString();
  }
  
  private String buildLink() {
    
    String topicId = getActivityParamValue(ForumActivityBuilder.TOPIC_ID_KEY);
    String forumId = getActivityParamValue(ForumActivityBuilder.FORUM_ID_KEY);
    try {
      return BuildLinkUtils.buildLink(forumId, topicId, PORTLET_INFO.FORUM);
    } catch (Exception ex) {
      return "";
    }
  }

  private String getLink(String tagLink, String nameLink) {
    String link = buildLink();
    return String.format(tagLink, link, nameLink);
  }
  
  public String getViewLink() {
    return buildLink();
  }
  

  public String getLastReplyLink() {
    String viewLink = buildLink();
    return (Utils.isEmpty(viewLink)) ? StringUtils.EMPTY : viewLink.concat("/lastpost");
  }

  protected String getViewPostLink(ExoSocialActivity activity) {
    String topicView = buildLink();
    Map<String, String> templateParams = activity.getTemplateParams();
    if(templateParams != null && templateParams.containsKey(ForumActivityBuilder.POST_ID_KEY)) {
      return topicView.concat("/").concat(templateParams.get(ForumActivityBuilder.POST_ID_KEY));
    }
    return StringUtils.EMPTY;
  }

  protected String getActivityContentTitle(WebuiBindingContext _ctx, String herf) throws Exception {
    String title = getActivity().getTitle();
    String linkTag = StringUtils.EMPTY;
    try {
      linkTag = getLink(herf, title);
    } catch (Exception e) {
      LOG.debug("Failed to get activity content and title ", e);
    }
    return linkTag;
  }
  
  public String getNumberOfReplies() {
    String got = getActivityParamValue(ForumActivityBuilder.TOPIC_POST_COUNT_KEY);
    if (Utils.isEmpty(got) && getTopic() != null) {
      got = "" + getTopic().getPostCount();
    }
    int nbReplies = Integer.parseInt(Utils.isEmpty(got) ? "0" : got);
    switch (nbReplies) {
      case 0:
        return WebUIUtils.getLabel(null, "ForumUIActivity.label.noReply");
      case 1:
        return WebUIUtils.getLabel(null, "ForumUIActivity.label.oneReply").replace("{0}", got);
      default:
        return WebUIUtils.getLabel(null, "ForumUIActivity.label.replies").replace("{0}", got);
    }
  }
  
  public double getRate() {
    String got = getActivityParamValue(ForumActivityBuilder.TOPIC_VOTE_RATE_KEY);
    if (Utils.isEmpty(got) && getTopic() != null) {
      got = "" + getTopic().getVoteRating();
    }
    try {
      return Double.parseDouble(got);
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }
  
  private Topic getTopic() {
    String topicId = getActivityParamValue(ForumActivityBuilder.TOPIC_ID_KEY);
    try {
      String categoryId = getActivityParamValue(ForumActivityBuilder.CATE_ID_KEY);
      String forumId = getActivityParamValue(ForumActivityBuilder.FORUM_ID_KEY);
      return ForumActivityUtils.getForumService().getTopic(categoryId, forumId, topicId, "");
    } catch (Exception e) {
      try {
        return (Topic) ForumActivityUtils.getForumService().getObjectNameById(topicId, Utils.TOPIC);
      } catch (Exception e2) {
        return null;
      }
    }
  }
  
  public boolean isTopicActivity() {
    if (Utils.isEmpty(getActivityParamValue(ForumActivityBuilder.TOPIC_ID_KEY)) == false) {
      return true;
    }
    return false;
  }
  
  public boolean isLockedOrClosed() {
    Topic topic = getTopic();
    if(topic == null || topic.getIsClosed() || topic.getIsLock()){
      return true;
    }
    Forum forum = ForumActivityUtils.getForumService().getForum(topic.getCategoryId(), topic.getForumId());
    
    if (forum == null) {
      return true;
    }
    
    if(forum.getIsClosed() || forum.getIsLock()){
      return true;
    }
    return false;
  }
  
  public Post createPost(String message, WebuiRequestContext requestContext) {
    try {
      Topic topic = getTopic();
      //
      Post post = new Post();
      post.setOwner(requestContext.getRemoteUser());
      post.setIcon("IconsView");
      post.setName("Re: " + topic.getTopicName());
      post.setLink(topic.getLink());

      //
      PortalRequestContext context = Util.getPortalRequestContext();
      String remoteAddr = ((HttpServletRequest) context.getRequest()).getRemoteAddr();

      post.setRemoteAddr(remoteAddr);

      post.setModifiedBy(requestContext.getRemoteUser());
      //
      message = message.replace("<p>", "").replace("</p>", "\n");
      post.setMessage(TransformHTML.enCodeHTMLContent(message));

      getApplicationComponent(DataStorage.class).savePost(topic.getCategoryId(), topic.getForumId(), topic.getId(), post, true, new MessageBuilder());

      //
      ExoSocialActivity activity = getActivity();
      activity = ForumActivityBuilder.updateNumberOfReplies(activity, false);
      activity.setTitle(null);
      activity.setBody(null);
      ForumActivityUtils.updateActivities(activity);

      return post;
    } catch (Exception e) {
      return null;
    }
  }
  
  public static class PostCommentActionListener extends BaseUIActivity.PostCommentActionListener {
    @Override
    public void execute(Event<BaseUIActivity> event) throws Exception {
      ForumUIActivity uiActivity = (ForumUIActivity) event.getSource();
      if (uiActivity.isTopicActivity() == false) {
        super.execute(event);
        return;
      }
      
      WebuiRequestContext requestContext = event.getRequestContext();
      UIFormTextAreaInput uiFormComment = uiActivity.getChild(UIFormTextAreaInput.class);
      String message = uiFormComment.getValue();
      uiFormComment.reset();
      
      //
      Post post = uiActivity.createPost(message, requestContext);
        boolean isMigratedActivity = false;

         //Case of migrate activity, post will be null
           if (post == null) {
            post = new Post();
            isMigratedActivity = true;
          }
      //
      post.setMessage(message);
      uiActivity.saveComment(post, isMigratedActivity);

      uiActivity.setCommentFormFocused(true);
      requestContext.addUIComponentToUpdateByAjax(uiActivity);

      uiActivity.getParent().broadcast(event, event.getExecutionPhase());
    }
  }
  
  /**
   * Create comment from post
   * @param post
   */
  private void saveComment(Post post, boolean isMigratedActivity) {
      ExoSocialActivity comment = new ExoSocialActivityImpl();

      if (isMigratedActivity == false) {
      ForumActivityContext ctx = ForumActivityContext.makeContextForAddPost(post);
     comment = ForumActivityBuilder.createActivityComment(ctx.getPost(), ctx);
     }
    comment.setUserId(org.exoplatform.social.webui.Utils.getViewerIdentity().getId());
    comment.setTitle(post.getMessage());
    comment.setBody(post.getMessage());
    ForumActivityUtils.getActivityManager().saveComment(getActivity(), comment);
      //Never save comment's id to a post when comment on a activity that is not applied activity-type specification
          if (isMigratedActivity == false) {
              ForumActivityUtils.takeCommentBack(post, comment);
             }
    refresh();
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

  protected String getSpaceGroupId() {
    return getActivityParamValue(SPACE_GROUP_ID);
  }
}
