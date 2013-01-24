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

import org.exoplatform.container.PortalContainer;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
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
 * Jan 10, 2013  
 */
public class ForumActivityUtils {

  private static final int   TYPE_PRIVATE      = 2;
  
  private static ForumService forumService;
  private static ActivityManager activityManager;
  private static IdentityManager identityManager;
  private static SpaceService spaceService;
  
  
  public static Identity getSpaceIdentity(String forumId) {
    String prettyname = forumId.replaceFirst(Utils.FORUM_SPACE_ID_PREFIX, "");
    Space space = getSpaceService().getSpaceByPrettyName(prettyname);
    Identity spaceIdentity = null;
    if (space != null) {
      spaceIdentity = getIdentityManager().getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);
    }
    return spaceIdentity;
  }
  
  public static boolean hasSpace(String forumId) throws Exception {
    return !Utils.isEmpty(forumId) && forumId.indexOf(Utils.FORUM_SPACE_ID_PREFIX) >= 0;
  }
  
  public static boolean isCategoryPublic(Category category) {
    // the category is public when it does not restrict viewers and private users.
    return category != null && Utils.isEmpty(category.getViewer()) && Utils.isEmpty(category.getUserPrivate());
  }
 
  public static boolean isForumPublic(Forum forum) {
    // the forum is public when it does not restrict viewers and is opening.
    return forum != null && !forum.getIsClosed() && Utils.isEmpty(forum.getViewer());
  }
  
  public static boolean isTopicPublic(Topic topic) {
    // the topic is public when it is active, not waiting, not closed yet and does not restrict users
    return topic != null && topic.getIsActive() && topic.getIsApproved() && !topic.getIsWaiting() && !topic.getIsClosed() && Utils.isEmpty(topic.getCanView());
  }
  
  public static boolean isPostPublic(Post post) {
    // the post is public when it is not private, not hidden by censored words, active by topic and not waiting for approval
    return post != null && post.getUserPrivate().length != TYPE_PRIVATE && !post.getIsWaiting() && !post.getIsHidden() && post.getIsActiveByTopic() && post.getIsApproved();
  }
  
  public static Topic getTopic(ForumActivityContext ctx) throws Exception {
    Post p = ctx.getPost();
    return ForumActivityUtils.getForumService().getTopic(p.getCategoryId(),
                                                         p.getForumId(),
                                                         p.getTopicId(),
                                                         "");
  }
  
  public static void takeActivityBack(Topic topic, ExoSocialActivity activity) {
    ForumActivityUtils.getForumService().saveActivityIdForOwnerPath(topic.getPath(), activity.getId());
  }
  
  public static void takeCommentBack(Post post, ExoSocialActivity comment) {
    ForumActivityUtils.getForumService().saveActivityIdForOwnerPath(post.getPath(), comment.getId());
  }
  
  /**
   * Gets ActivityId from existing Topic in Context.
   * If is NULL, create new Activity for Topic.
   * @param ctx
   * @return
   */
  public static String getActivityId(ForumActivityContext ctx) {
    ForumService fs = ForumActivityUtils.getForumService();
    String activityId = fs.getActivityIdForOwnerPath(ctx.getTopic().getPath());
    
    //
    if (Utils.isEmpty(activityId)) {
      TopicActivityTask task = TopicActivityTask.ADD_TOPIC;
      ExoSocialActivity got = ActivityExecutor.execute(task, ctx);
      
      //
      ForumActivityUtils.takeActivityBack(ctx.getTopic(), got);
      activityId = got.getId();
    }
    
    return activityId;
  }
  
  /**
   * Gets ActivityId from existing Topic in Context.
   * If is NULL, create new Activity for Topic.
   * @param ctx
   * @return
   */
  public static ExoSocialActivity getActivityOfTopic(ForumActivityContext ctx) {
    ForumService fs = ForumActivityUtils.getForumService();
    String activityId = fs.getActivityIdForOwnerId(ctx.getTopic().getId());
    
    ActivityManager am = ForumActivityUtils.getActivityManager();
    
    //
    if (Utils.isEmpty(activityId)) {
      return makeActivity(ctx);
    }
    
    //
    ExoSocialActivity got = am.getActivity(activityId);
    
    //
    if (got == null) {
      got = makeActivity(ctx);
      
    }
    
    
    return got;
  }
  
  private static ExoSocialActivity makeActivity(ForumActivityContext ctx) {
    TopicActivityTask task = TopicActivityTask.ADD_TOPIC;
    ExoSocialActivity got = ActivityExecutor.execute(task, ctx);
    
    //
    ForumActivityUtils.takeActivityBack(ctx.getTopic(), got);
    
    return got;
  }
  /**
   * Deletes Activities
   * 
   * @param activityIds
   */
  public static void removeActivities(String ... activityIds) {
    ActivityManager am = getActivityManager();
    for(String activityId : activityIds) {
      am.deleteActivity(activityId);
    }
  }
  
  public static ForumService getForumService() {
    if (forumService == null) {
      forumService = (ForumService) PortalContainer.getInstance().getComponentInstanceOfType(ForumService.class);
    }
    
    return forumService;
  }
  
  public static ActivityManager getActivityManager() {
    if (activityManager == null) {
      activityManager = (ActivityManager) PortalContainer.getInstance().getComponentInstanceOfType(ActivityManager.class);
    }
    return activityManager;
  }
  
  public static IdentityManager getIdentityManager() {
    if (identityManager == null) {
      identityManager = (IdentityManager) PortalContainer.getInstance().getComponentInstanceOfType(IdentityManager.class);
    }
    return identityManager;
  }
  
  public static SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = (SpaceService) PortalContainer.getInstance().getComponentInstanceOfType(SpaceService.class);
    }
    return spaceService;
  }
  
  public static Identity getIdentity(String remoteId) {
    return getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteId, false);
  }
}
