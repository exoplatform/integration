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
package org.exoplatform.forum.ext.forum;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.service.impl.model.PostFilter;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class ForumActivityTestCase extends BaseForumActivityTestCase {
  
  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }

  public void testForumService() throws Exception {
    assertNotNull(getForumService());
  }
  
  public void testSplitTopic() throws Exception {
    Topic topic = forumService.getTopic(categoryId, forumId, topicId, "");
    assertNotNull(topic);
    String activityId = forumService.getActivityIdForOwnerPath(topic.getPath());
    ExoSocialActivity activity = getActivityManager().getActivity(activityId);
    assertNotNull(activity);
    assertEquals(0, getActivityManager().getCommentsWithListAccess(activity).getSize());

    Post post1 = createdPost("name1", "message1");
    Post post2 = createdPost("name2", "message2");
    Post post3 = createdPost("name3", "message3");
    Post post4 = createdPost("name4", "message4");
    forumService.savePost(categoryId, forumId, topicId, post1, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post2, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post3, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post4, true, new MessageBuilder());
    
    activity = getActivityManager().getActivity(activityId);
    assertEquals(4, getActivityManager().getCommentsWithListAccess(activity).getSize());
    
    List<String> postPaths = new ArrayList<String>();
    postPaths.add(post1.getPath());
    postPaths.add(post2.getPath());
    postPaths.add(post3.getPath());
    postPaths.add(post4.getPath());
    Topic newTopic = createdTopic("root");
    newTopic.setId(post1.getId().replace("post", "topic"));
    newTopic.setOwner(post1.getOwner());
    newTopic.setPath(categoryId + "/" + forumId + "/" + post1.getId().replace("post", "topic"));
    //split topic and move post1-post2 to new topic
    forumService.splitTopic(newTopic, post1, postPaths, "", "");
    
    assertEquals(1, forumService.getPosts(new PostFilter(topic.getPath())).getSize());
    assertEquals(4, forumService.getPosts(new PostFilter(newTopic.getPath())).getSize());
    
    //2 actitivies created after split topic
    String activityId1 = forumService.getActivityIdForOwnerPath(topic.getPath());
    ExoSocialActivity activity1 = getActivityManager().getActivity(activityId1);
    assertNotNull(activity1);
    ListAccess<ExoSocialActivity> list = getActivityManager().getCommentsWithListAccess(activity1);
    assertEquals(0, list.getSize());
    //assertEquals("message3", list.load(0, 10)[0].getTitle());
    
    String activityId2 = forumService.getActivityIdForOwnerPath(newTopic.getPath());
    ExoSocialActivity activity2 = getActivityManager().getActivity(activityId2);
    assertNotNull(activity2);
    ListAccess<ExoSocialActivity> list2 = getActivityManager().getCommentsWithListAccess(activity2);
    assertEquals(3, list2.getSize());
    assertEquals("message2", list2.load(0, 10)[0].getTitle());
  }
  
  public void testMergeTopics() throws Exception {
    Forum forum = forumService.getForum(categoryId, forumId);
    assertNotNull(forum);
    
    //create 2 topic
    Topic topic1 = createdTopic("root");
    topic1.setDescription("topic 1");
    Topic topic2 = createdTopic("root");
    topic2.setDescription("topic 2");
    forumService.saveTopic(categoryId, forumId, topic1, true, false, new MessageBuilder());
    forumService.saveTopic(categoryId, forumId, topic2, true, false, new MessageBuilder());
    
    //get all post of topic1, include the first post
    PostFilter filter = new PostFilter(categoryId, forumId, topic1.getId(), "", "", "", "");
    ListAccess<Post> listPost = forumService.getPosts(filter);
    assertEquals(1, listPost.getSize());
    //get all post of topic2, include the first post
    filter = new PostFilter(categoryId, forumId, topic2.getId(), "", "", "", "");
    listPost = forumService.getPosts(filter);
    assertEquals(1, listPost.getSize());
    
    Identity rootIdentity = getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    List<ExoSocialActivity> activities = getActivityManager().getActivitiesWithListAccess(rootIdentity).loadAsList(0, 10);
    //there are 3 activities of root, 1 for topic created by default + 1 for topic1 + 1 for topic2
    assertEquals(3, activities.size());
    
    String topicPath1 = categoryId + "/" + forumId + "/" + topic1.getId();
    String topicPath2 = "/exo:applications/ForumService/ForumData/CategoryHome/" + categoryId + "/" + forumId + "/" + topic2.getId();
    forumService.mergeTopic(topicPath1, topicPath2, "", "", "topicMerged");
    
    listPost = forumService.getPosts(filter);
    assertEquals(2, listPost.getSize());
    assertEquals("topic 1", (listPost.load(0, 10)[1]).getMessage());
    
    activities = getActivityManager().getActivitiesWithListAccess(rootIdentity).loadAsList(0, 10);
    assertEquals(2, activities.size());
    
    String activityId = forumService.getActivityIdForOwnerPath(topic2.getPath());
    ExoSocialActivity activity = getActivityManager().getActivity(activityId);
    assertNotNull(activity);
    assertEquals(1, getActivityManager().getCommentsWithListAccess(activity).getSize());
    assertEquals("topic 1", getActivityManager().getCommentsWithListAccess(activity).load(0, 10)[0].getTitle());
    
    //Create new topic with 2 posts
    Topic topic3 = createdTopic("root");
    topic3.setDescription("topic 3");
    forumService.saveTopic(categoryId, forumId, topic3, true, false, new MessageBuilder());
    Post post1 = createdPost("name1", "message1");
    Post post2 = createdPost("name2", "message2");
    forumService.savePost(categoryId, forumId, topic3.getId(), post1, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topic3.getId(), post2, true, new MessageBuilder());
    
    //merge topic3 into topic2
    String topicPath3 = categoryId + "/" + forumId + "/" + topic3.getId();
    forumService.mergeTopic(topicPath3, topicPath2, "", "", "topicMerged");
    
    //topic will now have 5 posts
    filter = new PostFilter(categoryId, forumId, topic2.getId(), "", "", "", "");
    listPost = forumService.getPosts(filter);
    assertEquals(5, listPost.getSize());
    
    //check activity after merge
    activities = getActivityManager().getActivitiesWithListAccess(rootIdentity).loadAsList(0, 10);
    assertEquals(2, activities.size());
    
    activityId = forumService.getActivityIdForOwnerPath(topic2.getPath());
    activity = getActivityManager().getActivity(activityId);
    assertNotNull(activity);
    assertEquals(4, getActivityManager().getCommentsWithListAccess(activity).getSize());
    assertEquals("topic 1", getActivityManager().getCommentsWithListAccess(activity).load(0, 10)[0].getTitle());
    assertEquals("topic 3", getActivityManager().getCommentsWithListAccess(activity).load(0, 10)[1].getTitle());
    assertEquals("message1", getActivityManager().getCommentsWithListAccess(activity).load(0, 10)[2].getTitle());
    assertEquals("message2", getActivityManager().getCommentsWithListAccess(activity).load(0, 10)[3].getTitle());
  }

  private ActivityManager getActivityManager() {
    return (ActivityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ActivityManager.class);
  }
  
  private IdentityManager getIdentityManager() {
    return (IdentityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(IdentityManager.class);
  }

}
