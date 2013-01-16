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

import java.util.Date;
import java.util.Map;
import java.util.Random;

import junit.framework.TestCase;

import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.services.idgenerator.IDGeneratorService;
import org.exoplatform.services.idgenerator.impl.IDGeneratorServiceImpl;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 14, 2013  
 */
public class AbstractActivityTypeTest extends TestCase {

  private IdGenerator ig;
  @Override
  protected void setUp() throws Exception {
    IDGeneratorService is = new IDGeneratorServiceImpl();
    ig = new IdGenerator(is);
    super.setUp();
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    ig = null;
  }
  
  protected final String topicTitle = "topic title";
  protected final String topicContent = "topic content";
  
  protected Topic lockTopic() {
    Topic topic = createdTopic("demo");
    topic.setEditedIsLock(true);
    return topic;
  }
  
  protected Topic unlockTopic() {
    Topic topic = createdTopic("demo");
    topic.setIsLock(true);
    topic.setEditedIsLock(false);
    return topic;
  }
  
  protected Topic censoringTopic() {
    Topic topic = createdTopic("demo");
    topic.setEditedIsWaiting(true);
    return topic;
  }
  
  protected Topic uncensoringTopic() {
    Topic topic = createdTopic("demo");
    topic.setIsWaiting(true);
    topic.setEditedIsWaiting(false);
    return topic;
  }
  
  protected Topic activeTopic() {
    Topic topic = createdTopic("demo");
    topic.setEditedIsActive(true);
    return topic;
  }
  
  protected Topic updateTopicName(Topic topic) {
    topic.setEditedTopicName("new topic name");
    return topic;
  }
  
  protected Topic updateTopicDescription(Topic topic) {
    topic.setEditedDescription("new topic description.");
    return topic;
  }
  
  public void assertNumberOfReplies(ExoSocialActivity activity, long expectedNumber) {
   Map<String, String> templateParams = activity.getTemplateParams();
    
   String got = templateParams.get(ForumActivityBuilder.TOPIC_POST_COUNT_KEY);
   assertEquals("" + expectedNumber, got);
  }
  
  public void assertVoteRate(ExoSocialActivity activity, double expectedNumber) {
    Map<String, String> templateParams = activity.getTemplateParams();
     
    String got = templateParams.get(ForumActivityBuilder.TOPIC_VOTE_RATE_KEY);
    assertEquals("" + expectedNumber, got);
   }
  
  public void assertTopicTitle(ExoSocialActivity activity, String expectedTitle) {
    assertEquals(expectedTitle, activity.getTitle());
  }
  
  public void assertTopicContent(ExoSocialActivity activity, String expectedContent) {
    assertEquals(expectedContent, activity.getBody());
  }
  
  protected Topic updateTopicTitle(Topic topic, String newTitle) {
    topic.setEditedTopicName(newTitle);
    return topic;
  }
  
  protected Topic updateTopicContent(Topic topic, String newContent) {
    topic.setEditedDescription(newContent);
    return topic;
  }
  
  protected Topic createdTopic(String owner) {
    Topic topicNew = new Topic();

    topicNew.setOwner(owner);
    topicNew.setTopicName(topicTitle);
    topicNew.setCreatedDate(new Date());
    topicNew.setModifiedBy("root");
    topicNew.setModifiedDate(new Date());
    topicNew.setLastPostBy("root");
    topicNew.setLastPostDate(new Date());
    topicNew.setDescription(topicContent);
    topicNew.setPostCount(0);
    topicNew.setViewCount(0);
    topicNew.setIsNotifyWhenAddPost("");
    topicNew.setIsModeratePost(false);
    topicNew.setIsClosed(false);
    topicNew.setIsLock(false);
    topicNew.setIsWaiting(false);
    topicNew.setIsActive(true);
    topicNew.setIcon("classNameIcon");
    topicNew.setIsApproved(true);
    topicNew.setCanView(new String[] {});
    topicNew.setCanPost(new String[] {});
    return topicNew;
  }

  protected Forum createdForum() {
    Forum forum = new Forum();
    forum.setOwner("root");
    forum.setForumName("TestForum");
    forum.setForumOrder(1);
    forum.setCreatedDate(new Date());
    forum.setModifiedBy("root");
    forum.setModifiedDate(new Date());
    forum.setLastTopicPath("");
    forum.setDescription("description");
    forum.setPostCount(0);
    forum.setTopicCount(0);

    forum.setNotifyWhenAddTopic(new String[] {});
    forum.setNotifyWhenAddPost(new String[] {});
    forum.setIsModeratePost(false);
    forum.setIsModerateTopic(false);
    forum.setIsClosed(false);
    forum.setIsLock(false);

    forum.setViewer(new String[] {});
    forum.setCreateTopicRole(new String[] {});
    forum.setModerators(new String[] {});
    return forum;
  }

  protected Category createCategory(String id) {
    Category cat = new Category(id);
    cat.setOwner("root");
    cat.setCategoryName("testCategory");
    cat.setCategoryOrder(1);
    cat.setCreatedDate(new Date());
    cat.setDescription("desciption");
    cat.setModifiedBy("root");
    cat.setModifiedDate(new Date());
    return cat;
  }

  protected String getId(String type) {
    try {
      return type + IdGenerator.generate();
    } catch (Exception e) {
      return type + String.valueOf(new Random().nextLong());
    }
  }
}
