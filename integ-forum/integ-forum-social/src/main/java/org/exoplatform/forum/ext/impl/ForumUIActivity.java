package org.exoplatform.forum.ext.impl;

import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.forum.ext.activity.ForumActivityBuilder;
import org.exoplatform.forum.ext.activity.ForumActivityContext;
import org.exoplatform.forum.ext.activity.ForumActivityUtils;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.DataStorage;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
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
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.processor.I18NActivityProcessor;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
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
    @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class, confirm = "UIActivity.msg.Are_You_Sure_To_Delete_This_Activity"),
    @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class, confirm = "UIActivity.msg.Are_You_Sure_To_Delete_This_Comment") })
public class ForumUIActivity extends BaseKSActivity {

  private static final Log LOG = ExoLogger.getLogger(ForumUIActivity.class);
  
  private static final String SPACES_GROUP = SpaceUtils.SPACE_GROUP.substring(1);
  private static final String FORUM_PAGE_NAGVIGATION = "forum";
  private static final String FORUM_PORTLET_NAME = "ForumPortlet";
  private static final String SPACE_GROUP_ID  = "SpaceGroupId";

  public ForumUIActivity() {
    
  }

  /*
   * used by template, see line 201 ForumUIActivity.gtmpl
   */
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
    String categoryId = getActivityParamValue(ForumActivityBuilder.CATE_ID_KEY);
    String forumId = getActivityParamValue(ForumActivityBuilder.FORUM_ID_KEY);
    
    try {
      ForumService fs = ForumActivityUtils.getForumService();
      Category cate = fs.getCategory(categoryId);
      
      String link = "";
      //
      if (cate.getId().indexOf(SPACES_GROUP) > 0) {
        Forum forum = fs.getForum(categoryId, forumId);
        String spaceGroupId = ForumActivityUtils.getSpaceGroupId(forum.getId());
        link = buildTopicLink(spaceGroupId, topicId);
      } else {
        PortalRequestContext prc = Util.getPortalRequestContext();

        UserPortal userPortal = prc.getUserPortal();
        UserNavigation userNav = userPortal.getNavigation(prc.getSiteKey());
        UserNode userNode = userPortal.getNode(userNav, Scope.ALL, null, null);
        
        //
        UserNode forumNode = userNode.getChild(FORUM_PAGE_NAGVIGATION);
        
        //
        if (forumNode != null) {
          String forumURI = getNodeURL(forumNode);
          link = String.format("%s/topic/%s", forumURI, topicId);
        }
      }

      //
      LOG.info("Link " + link);
      return link;
    } catch (Exception ex) {
      return "";
    }
  }

  private String getLink(String tagLink, String nameLink) {
    String link = buildLink();
    return String.format(tagLink, link, nameLink);
  }
  
  private String getNodeURL(UserNode node) {
    RequestContext ctx = RequestContext.getCurrentInstance();
    NodeURL nodeURL =  ctx.createURL(NodeURL.TYPE);
    return nodeURL.setNode(node).toString();
  }
  
  public String buildTopicLink(String spaceGroupId, String topicId) throws Exception{
    
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
        if (FORUM_PAGE_NAGVIGATION.equals(child.getName()) || child.getName().indexOf(FORUM_PORTLET_NAME) >= 0) {
          break;
        }
        LOG.info("Node name = " + child.getName());
      }
      String spaceLink = getSpaceHomeURL(spaceGroupId);
      String topicLink = String.format("%s/%s/topic/%s", spaceLink, child.getName(), topicId);
      
      return topicLink;
    }
   
    return "";
  }
  
  /**
   * Gets the space home url of a space.
   * 
   * @param space
   * @return
   * @since 4.x
   */
  public static String getSpaceHomeURL(String spaceGroupId) {
    // work-around for SOC-2366 when rename existing space
    String permanentSpaceName = spaceGroupId.split("/")[2];
    Space space = ForumActivityUtils.getSpaceService().getSpaceByGroupId(spaceGroupId);
    
    NodeURL nodeURL =  RequestContext.getCurrentInstance().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.GROUP, SpaceUtils.SPACE_GROUP + "/"
                                        + permanentSpaceName, space.getPrettyName());
   
    return nodeURL.setResource(resource).toString(); 
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

  /*
   * used by Template, line 160 ForumUIActivity.gtmpl
   */
  @SuppressWarnings("unused")
  private String getActivityContentTitle(WebuiBindingContext _ctx, String herf) throws Exception {
    String title = getActivity().getTitle();
    String linkTag = "";
    try {
      linkTag = getLink(herf, title);
    } catch (Exception e) { // WebUIBindingContext
      LOG.debug("Failed to get activity content and title ", e);
    }
    return linkTag;
  }
  
  public String getNumberOfReplies() {
    ExoSocialActivity activity = getActivity();
    Map<String, String> templateParams = activity.getTemplateParams();
    
    String got = templateParams.get(ForumActivityBuilder.TOPIC_POST_COUNT_KEY);
    int nbReplies = Integer.parseInt(got);
    switch (nbReplies) {
      case 0:
        return "No Reply";
      case 1:
        return "1 Reply";
      default:
        return String.format("%s Replies", got);
    }
  }
  
  public String getRate() {
    ExoSocialActivity activity = getActivity();
    Map<String, String> templateParams = activity.getTemplateParams();
    
    String got = templateParams.get(ForumActivityBuilder.TOPIC_VOTE_RATE_KEY);
    return String.format("Rate: %s", got);
  }
  
  public boolean isTopicActivity() {
    String value = getActivityParamValue(ForumActivityBuilder.TOPIC_ID_KEY);
    if (value != null && value.length() > 0) {
      return true;
    }
    return false;
  }
  
  public Post createPost(String message, WebuiRequestContext requestContext) {
    try {
      DataStorage dataStorage = (DataStorage) PortalContainer.getInstance().getComponentInstanceOfType(DataStorage.class);
      String topicId = getActivityParamValue(ForumActivityBuilder.TOPIC_ID_KEY);
      String categoryId = getActivityParamValue(ForumActivityBuilder.CATE_ID_KEY);
      String forumId = getActivityParamValue(ForumActivityBuilder.FORUM_ID_KEY);
      Topic topic = dataStorage.getTopic(categoryId, forumId, topicId, "");

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
      post.setMessage(message);

      dataStorage.savePost(categoryId, forumId, topicId, post, true, new MessageBuilder());

      //
      ExoSocialActivity activity = getActivity();
      Map<String, String> templateParams = activity.getTemplateParams();
      templateParams.put(ForumActivityBuilder.TOPIC_POST_COUNT_KEY, String.valueOf(topic.getPostCount() + 1));

      activity.setTemplateParams(templateParams);
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

      //
      uiActivity.saveComment(post);

      uiActivity.setCommentFormFocused(true);
      requestContext.addUIComponentToUpdateByAjax(uiActivity);

      uiActivity.getParent().broadcast(event, event.getExecutionPhase());
    }
  }
  
  /**
   * Create comment from post
   * @param post
   */
  private void saveComment(Post post) {
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddPost(post);
    ExoSocialActivity comment = ForumActivityBuilder.createActivityComment(ctx.getPost(), ctx);
    comment.setUserId(org.exoplatform.social.webui.Utils.getViewerIdentity().getId());
    comment.setTitle(ForumActivityBuilder.getThreeFirstLines(post));
    comment.setBody(post.getMessage());
    org.exoplatform.social.webui.Utils.getActivityManager().saveComment(getActivity(), comment);
    //
    ForumActivityUtils.takeCommentBack(post, comment);
    
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
    return activity;
  }

  @SuppressWarnings("unused")
  private String getSpaceGroupId() {
    return getActivityParamValue(SPACE_GROUP_ID);
  }
}
