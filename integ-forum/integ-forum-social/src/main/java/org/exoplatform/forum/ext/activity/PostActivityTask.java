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

import java.util.Map;

import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
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
public abstract class PostActivityTask implements ActivityTask<ForumActivityContext> {

  protected static final Log   LOG = ExoLogger.getExoLogger(PostActivityTask.class);
  
  @Override
  public void start(ForumActivityContext ctx) { }
  
  @Override
  public void end(ForumActivityContext ctx) { }
  
  protected abstract ExoSocialActivity processTitle(ExoSocialActivity activity);
  protected abstract ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity activity);
  
  protected ExoSocialActivity processComment(ForumActivityContext ctx, ExoSocialActivity comment) {
    return processTitle(comment); 
  }
  
  public static PostActivityTask ADD_POST = new PostActivityTask() {

    @Override
    public ExoSocialActivity processTitle(ExoSocialActivity activity) {
      return ForumActivityType.ADD_POST.getActivity(activity, activity.getTitle());
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity topicActivity) {
      Map<String, String> templateParams = topicActivity.getTemplateParams();
      
      templateParams.put(ForumActivityBuilder.TOPIC_POST_COUNT_KEY, "" + ctx.getTopic().getPostCount());
      return topicActivity;
    };

    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        Topic topic = ForumActivityUtils.getTopic(ctx);
        ctx.setTopic(topic);
        
        //FORUM_33 case: update topic activity's number of reply 
        ExoSocialActivity topicActivity = ForumActivityUtils.getActivityOfTopic(ctx);
        
        Map<String, String> templateParams = topicActivity.getTemplateParams();
        templateParams.put(ForumActivityBuilder.TOPIC_POST_COUNT_KEY, "" + topic.getPostCount());
        
        ActivityManager am = ForumActivityUtils.getActivityManager();
        am.updateActivity(topicActivity);
        
        //add new comment with title: first 3 lines
        ExoSocialActivity newComment = ForumActivityBuilder.createActivityComment(ctx.getPost(), ctx);
        newComment = processComment(ctx, newComment);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getPost().getOwner());
        newComment.setUserId(poster.getId());
        
        //
        am.saveComment(topicActivity, newComment);
        
        return newComment;
      } catch (Exception e) {
        LOG.error("Can not record Comment for when add post " + ctx.getPost().getId(), e);
      }
      return null;
    }
    
  };
  
  public static PostActivityTask UPDATE_POST = new PostActivityTask() {
    
    @Override
    protected ExoSocialActivity processTitle(ExoSocialActivity activity) {
      //where $value is first 3 lines of the reply
      return ForumActivityType.UPDATE_POST.getActivity(activity, activity.getBody());
    }
    
    @Override
    protected ExoSocialActivity processActivity(ForumActivityContext ctx, ExoSocialActivity topicActivity) {
      return topicActivity;
    };
    
    @Override
    public ExoSocialActivity execute(ForumActivityContext ctx) {
      try {
        Topic topic = ForumActivityUtils.getTopic(ctx);
        ctx.setTopic(topic);
        
        //FORUM_34 case: update activity's title
        //add comment for updated post 
        ExoSocialActivity topicActivity = ForumActivityUtils.getActivityOfTopic(ctx);
        
        //
        ActivityManager am = ForumActivityUtils.getActivityManager();
        ExoSocialActivity newComment = ForumActivityBuilder.createActivityComment(ctx.getPost(), ctx);
        newComment = processComment(ctx, newComment);
        
        //
        Identity poster = ForumActivityUtils.getIdentity(ctx.getPost().getOwner());
        newComment.setUserId(poster.getId());
        
        am.saveComment(topicActivity, newComment);
          
        return topicActivity;
      } catch (Exception e) {
        LOG.error("Can not record Comment when updates post " + ctx.getPost().getId(), e);
      }
      
      return null;
    }
    
  };
}
