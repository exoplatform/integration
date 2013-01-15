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

import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 9, 2013  
 */
public abstract class TopicActivityTask implements ActivityTask<ForumActivityContext> {
  protected static final Log   LOG = ExoLogger.getExoLogger(TopicActivityTask.class);

  /**
   * 
   * @param activity
   * @return
   */
  abstract ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity);
    
  protected ExoSocialActivity processActivity(ForumActivityContext ctx) {
    ExoSocialActivity activity = ForumActivityBuilder.createActivity(ctx.getTopic(), ctx);
    return processTitle(ctx, activity); 
  }
  
  protected ExoSocialActivity processComment(ForumActivityContext ctx) {
    ExoSocialActivity activity = ForumActivityBuilder.createActivityComment(ctx.getTopic(), ctx);
    return processTitle(ctx, activity); 
  }

  public static TopicActivityTask ADD_TOPIC = new TopicActivityTask() {
    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.ADD_TOPIC.getActivity(activity, null);
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx) {
      ExoSocialActivity newActivity = ForumActivityBuilder.createActivity(ctx.getTopic(), ctx);
      //censoring status, hidden topic's activity in stream
      if (ctx.getTopic().getIsWaiting()) {
        newActivity.isHidden(true);
      }
      return processTitle(ctx, newActivity); 
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {

        ActivityManager am = ForumActivityUtils.getActivityManager();
        Identity streamOwner = getOwnerStream(ctx);
        
        ////FORUM_01 case: creates topic
        ExoSocialActivity newActivity = processActivity(ctx);
        
        
        am.saveActivityNoReturn(streamOwner, newActivity);
        
        return newActivity;
      } catch (Exception e) {
        LOG.error("Can not record Activity for when add topic's title " + ctx.getTopic().getId(), e);
      }
      return null;
    }
    
  };
  
  public static TopicActivityTask UPDATE_TOPIC_TITLE = new TopicActivityTask() {

    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.UPDATE_TOPIC_TITLE.getActivity(activity, activity.getTitle());
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ForumService fs = ForumActivityUtils.getForumService();
        String activityId = fs.getActivityIdForOwnerPath(ctx.getTopic().getPath());

        //FORUM_12 case: update topic's title
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = am.getActivity(activityId);
        ExoSocialActivity newComment = processComment(ctx);
        
        //
        a.setTitle(newComment.getTitle());
        am.updateActivity(a);
        
        //
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when update topic " + ctx.getTopic().getId(), e);
      }
      return null;
    }
    
  };
  
  public static TopicActivityTask UPDATE_TOPIC_CONTENT = new TopicActivityTask() {

    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.UPDATE_TOPIC_CONTENT.getActivity(activity, activity.getTitle());
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ForumService fs = ForumActivityUtils.getForumService();
        String activityId = fs.getActivityIdForOwnerPath(ctx.getTopic().getPath());

        //FORUM_13 case: update topic's content
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = am.getActivity(activityId);
        ExoSocialActivity newComment = processComment(ctx);
        
        //
        a.setTitle(newComment.getTitle());
        am.updateActivity(a);
        
        //
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when update topic's content " + ctx.getTopic().getId(), e);
      }
      return null;
    }
    
  };
  
  public static TopicActivityTask CLOSE_TOPIC = new TopicActivityTask() {

    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.CLOSE_TOPIC.getActivity(activity, null);
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ForumService fs = ForumActivityUtils.getForumService();
        String activityId = fs.getActivityIdForOwnerPath(ctx.getTopic().getPath());

        //FORUM_15 case: close topic
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = am.getActivity(activityId);
        a.isLocked(true);
        am.updateActivity(a);
        
        ExoSocialActivity newComment = processComment(ctx);
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when close topic " + ctx.getTopic().getId(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask OPEN_TOPIC = new TopicActivityTask() {

    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.OPEN_TOPIC.getActivity(activity, null);
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {

        ActivityManager am = ForumActivityUtils.getActivityManager();
        Identity streamOwner = getOwnerStream(ctx);
        
        //Don't have specs open a closed topic
        ExoSocialActivity newActivity = processComment(ctx);
        am.saveActivityNoReturn(streamOwner, newActivity);
        
        return newActivity;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when open topic " + ctx.getTopic().getId(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask LOCK_TOPIC = new TopicActivityTask() {

    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.LOCK_TOPIC.getActivity(activity, null);
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ForumService fs = ForumActivityUtils.getForumService();
        String activityId = fs.getActivityIdForOwnerPath(ctx.getTopic().getPath());

        //FORUM_16 case: lock topic
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = am.getActivity(activityId);
        a.isLocked(true);
        am.updateActivity(a);
        
        ExoSocialActivity newComment = processComment(ctx);
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when lock topic " + ctx.getTopic().getId(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask UNLOCK_TOPIC = new TopicActivityTask() {

    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.UNLOCK_TOPIC.getActivity(activity, null);
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ForumService fs = ForumActivityUtils.getForumService();
        String activityId = fs.getActivityIdForOwnerPath(ctx.getTopic().getPath());

        //FORUM_16 case: lock topic
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = am.getActivity(activityId);
        a.isLocked(false);
        am.updateActivity(a);
        
        ExoSocialActivity newComment = processComment(ctx);
        am.saveComment(a, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when unlock topic " + ctx.getTopic().getId(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask HIDDEN_TOPIC = new TopicActivityTask() {

    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return activity;
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {

        ForumService fs = ForumActivityUtils.getForumService();
        String activityId = fs.getActivityIdForOwnerPath(ctx.getTopic().getPath());

        //FORUM_25: hidding into a topic
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = am.getActivity(activityId);
        a.isHidden(true);
        am.updateActivity(a);
        
        return a;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when hidden topic " + ctx.getTopic().getId(), e);
        return null;
      }
    }
  };
  
  public static TopicActivityTask CENSORING_TOPIC = new TopicActivityTask() {

    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return activity;
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ForumService fs = ForumActivityUtils.getForumService();
        String activityId = fs.getActivityIdForOwnerPath(ctx.getTopic().getPath());

        //FORUM_24 case: censoring topic
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = am.getActivity(activityId);
        a.isHidden(true);
        am.updateActivity(a);

       return a;
      } catch (Exception e) {
        LOG.error("Can not hide activity for censoring topic " + ctx.getTopic().getId(), e);
        return null;
      }
    }
  };
  
  public static TopicActivityTask UNCENSORING_TOPIC = new TopicActivityTask() {

    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return activity;
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ForumService fs = ForumActivityUtils.getForumService();
        String activityId = fs.getActivityIdForOwnerPath(ctx.getTopic().getPath());

        //FORUM_xx case: unscensoring topic
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = am.getActivity(activityId);
        a.isHidden(false);
        am.updateActivity(a);

       return a;
      } catch (Exception e) {
        LOG.error("Can not show activity for uncensoring topic " + ctx.getTopic().getId(), e);
        return null;
      }
    }
  };
  
  public static TopicActivityTask ACTIVE_TOPIC = new TopicActivityTask() {

    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return activity;
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        
        ForumService fs = ForumActivityUtils.getForumService();
        String activityId = fs.getActivityIdForOwnerPath(ctx.getTopic().getPath());

        //FORUM_26: showing into a topic
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = am.getActivity(activityId);
        a.isHidden(false);
        am.updateActivity(a);
        
        return a;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when active topic " + ctx.getTopic().getId(), e);
        return null;
      }
    }
  };
  
  public static TopicActivityTask MOVE_TOPIC = new TopicActivityTask() {

    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.MOVE_TOPIC.getActivity(activity, ctx.getToCategoryName(), ctx.getToForumName());
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ForumService fs = ForumActivityUtils.getForumService();
        String activityId = fs.getActivityIdForOwnerPath(ctx.getTopic().getPath());

        //
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity a = am.getActivity(activityId);

        ////FORUM_22 case: move topic
        ExoSocialActivity newComment = processComment(ctx);
        am.saveComment(a, newComment);
        return newComment;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when moves topic " + ctx.getTopic().getId(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask MERGE_TOPIC = new TopicActivityTask() {

    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.MERGE_TOPICS.getActivity(activity, null);
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ForumActivityUtils.removeActivities(ctx.getRemoveActivities());

        //
        ActivityManager am = ForumActivityUtils.getActivityManager();
        Identity streamOwner = getOwnerStream(ctx);
        
        ////FORUM_21 case: merge topic
        ExoSocialActivity newActivity = processActivity(ctx);
        am.saveActivityNoReturn(streamOwner, newActivity);
        
        return newActivity;
      } catch (Exception e) {
        LOG.error("Can not record Activity for merged topics " + ctx.getTopic().getId(), e);
      }
      return null;
    }
  };
  
  public static TopicActivityTask SPLIT_TOPIC = new TopicActivityTask() {

    @Override
    ExoSocialActivity processTitle(ForumActivityContext ctx, ExoSocialActivity activity) {
      return ForumActivityType.SPLIT_TOPIC.getActivity(activity, null);
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ForumActivityUtils.removeActivities(ctx.getRemoveActivities());

        //
        ActivityManager am = ForumActivityUtils.getActivityManager();
        Identity streamOwner = getOwnerStream(ctx);
        
        ////FORUM_01 case: creates topic
        ExoSocialActivity newActivity = processActivity(ctx);
        am.saveActivityNoReturn(streamOwner, newActivity);
        
        return newActivity;
      } catch (Exception e) {
        LOG.error("Can not record Activity for splited topic " + ctx.getTopic().getId(), e);
      }
      return null;
    }
  };
  
  @Override
  public void start(ForumActivityContext ctx) {}
  
  @Override
  public void end(ForumActivityContext ctx) { }
  
  
  protected Identity getOwnerStream(ForumActivityContext ctx) {
    Identity ownerStream = null;
    ForumService fs = ForumActivityUtils.getForumService();

    try {
      Identity userIdentity = ForumActivityUtils.getIdentity(ctx.getPost().getOwner());
      Topic topic = ForumActivityUtils.getTopic(ctx);
      
      if (ForumActivityUtils.isTopicPublic(topic)) {
        if (ForumActivityUtils.hasSpace(ctx.getForumId())) {
          // publish the activity in the space stream.
          ownerStream = ForumActivityUtils.getSpaceIdentity(ctx.getForumId());
        }
        if (ownerStream == null
            && ForumActivityUtils.isCategoryPublic(fs.getCategory(ctx.getCategoryId()))
            && ForumActivityUtils.isForumPublic(fs.getForum(ctx.getCategoryId(), ctx.getForumId()))) {
          ownerStream = userIdentity;
        }
        return ownerStream;
      }
    } catch (Exception e) { // ForumService
      LOG.error("Can not get OwnerStream for topic " + ctx.getTopic().getId(), e);
    }

    return null;
  }
}
