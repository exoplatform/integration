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

import org.exoplatform.forum.service.Topic;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 16, 2013  
 */
public class TopicActivityTest extends AbstractActivityTypeTest {
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testAddTopic() throws Exception {
    Topic topic = createdTopic("demo");
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddTopic(topic);
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    assertNumberOfReplies(a, 0);
    assertVoteRate(a, topic.getVoteRating());
    assertTopicTitle(a, topicTitle);
    assertTopicContent(a, topicContent);
  }
  
  public void testAddTopicWithJob() throws Exception {
    Topic topic = createdTopic("demo");
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddTopic(topic);
    TopicActivityTask task = TopicActivityTask.ADD_TOPIC;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    a = task.processActivity(ctx, a);
    assertNumberOfReplies(a, 0);
    assertVoteRate(a, topic.getVoteRating());
    assertTopicTitle(a, topicTitle);
    assertTopicContent(a, topicContent);
  }
  
  public void testAddTopicWaitingWithJob() throws Exception {
    Topic topic = createdTopic("demo");
    topic.setIsWaiting(true);
    ForumActivityContext ctx = ForumActivityContext.makeContextForAddTopic(topic);
    TopicActivityTask task = TopicActivityTask.ADD_TOPIC;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    a = task.processActivity(ctx, a);
    assertEquals(true, a.isHidden());
  }
  
  public void testUpdateTopicTitle() throws Exception {
    Topic topic = createdTopic("demo");
    topic = updateTopicTitle(topic, "edited to new title for topic.");
    assertEquals(1, topic.getChangeEvent().length);
    assertEquals(Topic.TOPIC_NAME, topic.getChangeEvent()[0].getPropertyName());
    
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    assertNumberOfReplies(a, 0);
    assertVoteRate(a, topic.getVoteRating());
    assertTopicTitle(a, "edited to new title for topic.");
    assertTopicContent(a, topicContent);
    
  }
  
  public void testUpdateTopicTitleWithJob() throws Exception {
    Topic topic = createdTopic("demo");
    topic = updateTopicTitle(topic, "edited to new title for topic.");
    ForumActivityContext ctx = ForumActivityContext.makeContextForUpdateTopic(topic);
    
    TopicActivityTask task = TopicActivityTask.UPDATE_TOPIC_TITLE;
    ExoSocialActivity a = ForumActivityBuilder.createActivity(topic, ctx);
    a = task.processActivity(ctx, a);
    assertNumberOfReplies(a, 0);
    assertVoteRate(a, topic.getVoteRating());
    assertTopicTitle(a, "Title has been updated to: edited to new title for topic.");
    assertTopicContent(a, topicContent);
  }
}
