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
import org.exoplatform.forum.service.MessageBuilder;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.forum.service.impl.model.PostFilter;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
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
    ExoSocialActivity activity = getManager().getActivity(activityId);
    assertNotNull(activity);
    assertEquals(0, getManager().getCommentsWithListAccess(activity).getSize());

    Post post1 = createdPost("name1", "message1");
    Post post2 = createdPost("name2", "message2");
    Post post3 = createdPost("name3", "message3");
    Post post4 = createdPost("name4", "message4");
    forumService.savePost(categoryId, forumId, topicId, post1, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post2, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post3, true, new MessageBuilder());
    forumService.savePost(categoryId, forumId, topicId, post4, true, new MessageBuilder());
    
    activity = getManager().getActivity(activityId);
    assertEquals(4, getManager().getCommentsWithListAccess(activity).getSize());
    
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
    
    //assertEquals(1, forumService.getPosts(new PostFilter(topic.getPath())).getSize());
    //assertEquals(4, forumService.getPosts(new PostFilter(newTopic.getPath())).getSize());
    
    //2 actitivies created after split topic
    String activityId1 = forumService.getActivityIdForOwnerPath(topic.getPath());
    ExoSocialActivity activity1 = getManager().getActivity(activityId1);
    assertNotNull(activity1);
    ListAccess<ExoSocialActivity> list = getManager().getCommentsWithListAccess(activity1);
    assertEquals(0, list.getSize());
    //assertEquals("message3", list.load(0, 10)[0].getTitle());
    
    String activityId2 = forumService.getActivityIdForOwnerPath(newTopic.getPath());
    ExoSocialActivity activity2 = getManager().getActivity(activityId2);
    assertNotNull(activity2);
    ListAccess<ExoSocialActivity> list2 = getManager().getCommentsWithListAccess(activity2);
    //assertEquals(3, list2.getSize());
    //assertEquals("message2", list2.load(0, 10)[0].getTitle());
  }

  private ActivityManager getManager() {
    return (ActivityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ActivityManager.class);
  }

}
