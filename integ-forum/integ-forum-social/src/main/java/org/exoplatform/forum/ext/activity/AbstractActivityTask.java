/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.ext.activity;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.forum.bbcode.core.ExtendedBBCodeProvider;
import org.exoplatform.forum.common.TransformHTML;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
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
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 9, 2013  
 */
public abstract class AbstractActivityTask<T> implements ActivityTask<T> {
  
  public static final String FORUM_APP_ID      = "ks-forum:spaces";

  public static final String FORUM_ID_KEY      = "ForumId";

  public static final String CATE_ID_KEY       = "CateId";

  public static final String ACTIVITY_TYPE_KEY = "ActivityType";

  public static final String POST_TYPE         = "Post";

  public static final String POST_ID_KEY       = "PostId";

  public static final String POST_OWNER_KEY    = "PostOwner";

  public static final String POST_LINK_KEY     = "PostLink";

  public static final String POST_NAME_KEY     = "PostName";

  public static final String TOPIC_ID_KEY      = "TopicId";

  public static final String TOPIC_OWNER_KEY   = "TopicOwner";

  public static final String TOPIC_LINK_KEY    = "TopicLink";

  public static final String TOPIC_NAME_KEY    = "TopicName";
  
  public static final int   TYPE_PRIVATE      = 2;
  
  protected static enum ACTIVITYTYPE {
    AddPost, AddTopic, UpdatePost, UpdateTopic
  }
  
  private ForumService forumService;
  private ActivityManager activityManager;
  private IdentityManager identityManager;
  private SpaceService spaceService;
  
  
  protected Identity getSpaceIdentity(String forumId) {
    String prettyname = forumId.replaceFirst(Utils.FORUM_SPACE_ID_PREFIX, "");
    Space space = getSpaceService().getSpaceByPrettyName(prettyname);
    Identity spaceIdentity = null;
    if (space != null) {
      spaceIdentity = getIdentityManager().getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
    }
    return spaceIdentity;
  }
  
  protected boolean hasSpace(String forumId) throws Exception {
    return !Utils.isEmpty(forumId) && forumId.indexOf(Utils.FORUM_SPACE_ID_PREFIX) >= 0;
  }
  
  protected boolean isCategoryPublic(Category category) {
    // the category is public when it does not restrict viewers and private users.
    return category != null && Utils.isEmpty(category.getViewer()) && Utils.isEmpty(category.getUserPrivate());
  }
 
  protected boolean isForumPublic(Forum forum) {
 // the forum is public when it does not restrict viewers and is opening.
    return forum != null && !forum.getIsClosed() && Utils.isEmpty(forum.getViewer());
  }
  
  protected boolean isTopicPublic(Topic topic) {
    // the topic is public when it is active, not waiting, not closed yet and does not restrict users
    return topic != null && topic.getIsActive() && topic.getIsApproved() && !topic.getIsWaiting() && !topic.getIsClosed() && Utils.isEmpty(topic.getCanView());
  }
  
  protected boolean isPostPublic(Post post) {
    // the post is public when it is not private, not hidden by censored words, active by topic and not waiting for approval
    return post != null && post.getUserPrivate().length != TYPE_PRIVATE && !post.getIsWaiting() && !post.getIsHidden() && post.getIsActiveByTopic() && post.getIsApproved();
  }
  
  protected ExoSocialActivity createActivity(Identity author, String title, String body, String forumId, String categoryId, String topicId, String type, Map<String, String> templateParams) throws Exception {
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    body = StringEscapeUtils.unescapeHtml(TransformHTML.getTitleInHTMLCode(body, new ArrayList<String>((new ExtendedBBCodeProvider()).getSupportedBBCodes())));
    activity.setUserId(author.getId());
    title = StringEscapeUtils.unescapeHtml(title);
    activity.setTitle(title);
    activity.setBody(body);
    activity.setType(FORUM_APP_ID);
    templateParams.put(FORUM_ID_KEY, forumId);
    templateParams.put(CATE_ID_KEY, categoryId);
    templateParams.put(TOPIC_ID_KEY, topicId);
    templateParams.put(ACTIVITY_TYPE_KEY, type);
    activity.setTemplateParams(templateParams);
    return activity;
  }
  
  protected ForumService getForumService() {
    if (this.forumService == null) {
      this.forumService = (ForumService) PortalContainer.getInstance().getComponentInstanceOfType(ForumService.class);
    }
    
    return this.forumService;
  }
  
  protected ActivityManager getActivityManager() {
    if (this.activityManager == null) {
      this.activityManager = (ActivityManager) PortalContainer.getInstance().getComponentInstanceOfType(ActivityManager.class);
    }
    return this.activityManager;
  }
  
  protected IdentityManager getIdentityManager() {
    if (this.identityManager == null) {
      this.identityManager = (IdentityManager) PortalContainer.getInstance().getComponentInstanceOfType(IdentityManager.class);
    }
    return this.identityManager;
  }
  
  protected SpaceService getSpaceService() {
    if (this.spaceService == null) {
      this.spaceService = (SpaceService) PortalContainer.getInstance().getComponentInstanceOfType(SpaceService.class);
    }
    return this.spaceService;
  }
  
  protected Identity getIdentity(String remoteId) {
    return getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteId, false);
  }
}
