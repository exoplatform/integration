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
import org.exoplatform.forum.service.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 9, 2013  
 */
public abstract class PostActivityTask implements ActivityTask<ForumActivityContext> {

  protected static final Log   LOG = ExoLogger.getExoLogger(PostActivityTask.class);
  
  @Override
  public void start(ForumActivityContext ctx) { }
  
  @Override
  public void end(ForumActivityContext ctx) { }
  
  abstract ExoSocialActivity processTitle(ExoSocialActivity activity);
  
  protected ExoSocialActivity process(ForumActivityContext ctx) {
    ExoSocialActivity activity = ForumActivityBuilder.createActivityComment(ctx.getPost(), ctx);
    return processTitle(activity); 
  }
  
  public static PostActivityTask ADD_POST = new PostActivityTask() {

    @Override
    ExoSocialActivity processTitle(ExoSocialActivity activity) {
      return ForumActivityType.ADD_POST.getActivity(activity, activity.getTitle());
    }

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        Topic topic = ForumActivityUtils.getTopic(ctx);
        String activityId = ForumActivityUtils.getForumService().getActivityIdForOwnerPath(topic.getPath());
        
        ActivityManager am = ForumActivityUtils.getActivityManager();
        
        //
        if (Utils.isEmpty(activityId)) {
          LOG.error("Can not get Activity for topic " + ctx.getTopicId());
          return null;
        }
        
        ////FORUM_33 case: update topic activity's number of reply 
        ExoSocialActivity topicActivity = am.getActivity(activityId);
        am.updateActivity(topicActivity);
        
        //add new comment with title: first 3 lines
        ExoSocialActivity newComment = process(ctx);
        am.saveComment(topicActivity, newComment);
        
        return topicActivity;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when add post " + ctx.getPost().getId(), e);
      }
      return null;
    }
    
  };
  
  public static PostActivityTask UPDATE_POST = new PostActivityTask() {
    
    @Override
    ExoSocialActivity processTitle(ExoSocialActivity activity) {
      //where $value is first 3 lines of the reply
      return ForumActivityType.UPDATE_POST.getActivity(activity, activity.getBody());
    }
    
    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        ForumService fs = ForumActivityUtils.getForumService();
        Topic topic = fs.getTopic(ctx.getCategoryId(), ctx.getForumId(), ctx.getTopicId(), null);        
        String activityId = fs.getActivityIdForOwnerPath(topic.getPath());
        
        //
        if (Utils.isEmpty(activityId)) {
          LOG.error("Can not get Activity for topic " + ctx.getTopicId());
          return null;
        }
        
        //
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity topicActivity = am.getActivity(activityId);
        ExoSocialActivity newComment = process(ctx);
          
        //FORUM_34 case: update activity's title
        //add comment for updated post
        am.saveComment(topicActivity, newComment);
          
        return topicActivity;
      } catch (Exception e) {
        LOG.error("Can not record Comment when updates post " + ctx.getPost().getId(), e);
      }
      
      return null;
    }
    
  };
}
