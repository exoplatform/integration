/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.ext.impl;

import java.beans.PropertyChangeEvent;

import org.exoplatform.commons.utils.PropertyChangeSupport;
import org.exoplatform.forum.ext.activity.ActivityExecutor;
import org.exoplatform.forum.ext.activity.ForumActivityContext;
import org.exoplatform.forum.ext.activity.ForumActivityUtils;
import org.exoplatform.forum.ext.activity.PostActivityTask;
import org.exoplatform.forum.ext.activity.TopicActivityTask;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumEventListener;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

/**
 * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice Lamarque</a>
 * @version $Revision$
 */
public class ForumSpaceActivityPublisher extends ForumEventListener {

  @Override
  public void saveCategory(Category category) {
  }

  @Override
  public void saveForum(Forum forum) {
  }
  
 
  
  @Override
  public void addPost(Post post) {
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddPost(post);
    PostActivityTask task = PostActivityTask.ADD_POST;
    ExoSocialActivity comment = ActivityExecutor.execute(task, ctx);
    
    //
    ForumActivityUtils.takeCommentBack(post, comment);
  }
  
  @Override
  public void updatePost(Post post) {
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdatePost(post);
    PostActivityTask task = PostActivityTask.UPDATE_POST;
    ActivityExecutor.execute(task, ctx);
  }


  @Override
  public void addTopic(Topic topic) {
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddTopic(topic);
    TopicActivityTask task = TopicActivityTask.ADD_TOPIC;
    ExoSocialActivity got = ActivityExecutor.execute(task, ctx);
    
    //
    ForumActivityUtils.takeActivityBack(topic, got);
  }
  
  @Override
  public void updateTopic(Topic topic) {
    
    TopicActivityTask task = null;
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    
    updateProperties(topic, ctx);
    
    PropertyChangeEvent[] events = topic.getChangeEvent();
    
    for (int i = 0; i < events.length; i++) {
      task = getTaskFromUpdateTopic(events[i]);
      ActivityExecutor.execute(task, ctx);
    }
  }
  
  private void updateProperties(Topic topic, ForumActivityContext ctx) {
    PropertyChangeSupport newPcs = new PropertyChangeSupport(topic);
    PropertyChangeSupport pcs = topic.getPcs();
    
    //Topic.TOPIC_NAME
    if (pcs.hasPropertyName(Topic.TOPIC_NAME)) {
      newPcs.addPropertyChange(pcs.getPropertyChange(Topic.TOPIC_NAME));
    }
    
    //Topic.TOPIC_CONTENT
    if (pcs.hasPropertyName(Topic.TOPIC_CONTENT)) {
      newPcs.addPropertyChange(pcs.getPropertyChange(Topic.TOPIC_CONTENT));
    }
    
    if (newPcs.getChangeEvents().length > 1) {
      ctx.setPcs(newPcs);
      
      //1. executes task
      TopicActivityTask task = TopicActivityTask.UPDATE_TOPIC_PROPERTIES;
      ActivityExecutor.execute(task, ctx);
      //2. remove events
      pcs.removePropertyChange(Topic.TOPIC_NAME);
      pcs.removePropertyChange(Topic.TOPIC_CONTENT);
    }
    
  }
  
 
  @Override
  public void moveTopic(Topic topic, String toCategoryName, String toForumName) {
    ForumActivityContext ctx = ForumActivityContext.makeContextForMoveTopic(topic, toCategoryName, toForumName);
    TopicActivityTask task = TopicActivityTask.MOVE_TOPIC;
    ActivityExecutor.execute(task, ctx);
  }

  @Override
  public void mergeTopic(Topic newTopic, String removeActivityId1, String removeActivityId2) {
    ForumActivityContext ctx = ForumActivityContext.makeContextForMergeTopic(newTopic, removeActivityId1, removeActivityId2);
    TopicActivityTask task = TopicActivityTask.MERGE_TOPIC;
    ActivityExecutor.execute(task, ctx);
    
  }

  @Override
  public void splitTopic(Topic newTopic, Topic splitedTopic, String removeActivityId) {
    /** 1. Call delete activityId*/
    /** 2. Call add topic*/
    ForumActivityContext ctx = ForumActivityContext.makeContextForSplitTopic(newTopic, splitedTopic, removeActivityId);
    TopicActivityTask task = TopicActivityTask.SPLIT_TOPIC;
    ExoSocialActivity activity = ActivityExecutor.execute(task, ctx);
    
    //
    ForumActivityUtils.takeActivityBack(newTopic, activity);
  }
 
  @Override
  public void removeActivity(String activityId) {
    ForumActivityUtils.removeActivities(activityId);
  }
  
  private TopicActivityTask getTaskFromUpdateTopic(PropertyChangeEvent event) {
    TopicActivityTask got = null;
    if (Topic.TOPIC_NAME.equals(event.getPropertyName())) {
      got = TopicActivityTask.UPDATE_TOPIC_TITLE;
    } else if (Topic.TOPIC_CONTENT.equals(event.getPropertyName())) {
      got = TopicActivityTask.UPDATE_TOPIC_CONTENT;
    } else if (Topic.TOPIC_RATING.equals(event.getPropertyName())) {
      got = TopicActivityTask.UPDATE_TOPIC_RATE;
    } else if (Topic.TOPIC_STATE_CLOSED.equals(event.getPropertyName())) {
      
      //
      boolean isClose = (Boolean) event.getNewValue();
      if (isClose) {
        got = TopicActivityTask.CLOSE_TOPIC;
      } else {
        got = TopicActivityTask.OPEN_TOPIC;
      }
      
    } else if (Topic.TOPIC_STATUS_LOCK.equals(event.getPropertyName())) {
      
      //
      boolean isLock = (Boolean) event.getNewValue();
      if (isLock) {
        got = TopicActivityTask.LOCK_TOPIC;
      } else {
        got = TopicActivityTask.UNLOCK_TOPIC;
      }
   } else if (Topic.TOPIC_STATUS_APPROVED.equals(event.getPropertyName())) {
      
      //
      boolean isLock = (Boolean) event.getNewValue();
      if (isLock) {
        got = TopicActivityTask.APPROVED_TOPIC;
      } else {
        got = TopicActivityTask.UNAPPROVED_TOPIC;
      }
    
    } else if (Topic.TOPIC_STATUS_ACTIVE.equals(event.getPropertyName())) {
      
      //
      boolean isActive = (Boolean) event.getNewValue();
      if (isActive) {
        got = TopicActivityTask.ACTIVE_TOPIC;
      } else {
        got = TopicActivityTask.HIDDEN_TOPIC;
      }
    } else if (Topic.TOPIC_STATUS_WAITING.equals(event.getPropertyName())) {
      
      //
      boolean isWaiting = (Boolean) event.getNewValue();
      if (isWaiting) {
        got = TopicActivityTask.CENSORING_TOPIC;
      } else {
        got = TopicActivityTask.UNCENSORING_TOPIC;
      }
    }
    
    return got;
  }
  
}
