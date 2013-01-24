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
package org.exoplatform.forum.ext.poll;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.ext.impl.PollSpaceActivityPublisher;
import org.exoplatform.poll.service.Poll;
import org.exoplatform.poll.service.Poll.PollAction;
import org.exoplatform.poll.service.PollNodeTypes;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
public class PollServiceTestCase extends BaseTestCase {
  
  private List<Poll> tearDownPollList;
  
  PollSpaceActivityPublisher   listener = new PollSpaceActivityPublisher();
  
  private IdentityStorage identityStorage;
  private Identity rootIdentity;
  private Identity johnIdentity;

  public void setUp() throws Exception {
    super.setUp();
    identityStorage = (IdentityStorage) getContainer().getComponentInstanceOfType(IdentityStorage.class);
    rootIdentity = new Identity(OrganizationIdentityProvider.NAME, "root");
    johnIdentity = new Identity(OrganizationIdentityProvider.NAME, "john");
    identityStorage.saveIdentity(rootIdentity);
    identityStorage.saveIdentity(johnIdentity);
    pollService.addListenerPlugin(listener);
    tearDownPollList = new ArrayList<Poll>();
  }

  public void tearDown() throws Exception {
    for (Poll poll : tearDownPollList) {
      pollService.removePoll(poll.getId());
    }
    pollService.removeListenerPlugin(listener);
    identityStorage.deleteIdentity(rootIdentity);
    identityStorage.deleteIdentity(johnIdentity);
    super.tearDown();
  }

  public void testPollService() throws Exception {
    assertNotNull(getPollService());
  }

  /**
  * testSavePollWithActivity
  * 
  * @throws Exception
  */
  public void testSavePollWithActivity() throws Exception {
    // if poll of topic : parentPath = topic.getPath();
    Poll pollTopic = new Poll();
    pollTopic.setQuestion("What color?");
    String[] options = { "red", "blue" };
    pollTopic.setOption(options);
    pollTopic.setOwner(rootIdentity.getRemoteId());
    pollTopic.setParentPath(topicPath);
    pollTopic.setPollAction(PollAction.Create_Poll);

    // When create poll, an activity will be save
    pollService.savePoll(pollTopic, true, false);
    String activityId = pollService.getActivityIdForOwner(pollTopic.getParentPath() + "/"+ pollTopic.getId());
    ExoSocialActivity activity = getManager().getActivity(activityId);
    assertNotNull(activity);
    assertEquals("What color?", activity.getTitle());
    // Number of comments must be 0
    assertEquals(0, getManager().getCommentsWithListAccess(activity).getSize());

    // update poll
    pollTopic.setQuestion("Hello");
    pollTopic.setPollAction(PollAction.Update_Poll);
    pollService.savePoll(pollTopic, false, false);
    activity = getManager().getActivity(activityId);
    List<ExoSocialActivity> comments = getManager().getCommentsWithListAccess(activity).loadAsList(0, 10);
    // Number of comments must be 1
    assertEquals(1, comments.size());
    assertEquals("Poll has been updated.", comments.get(0).getTitle());

    // vote poll
    String[] userVote = { "root:1" };
    pollTopic.setUserVote(userVote);
    pollTopic.setPollAction(PollAction.Vote_Poll);
    pollService.savePoll(pollTopic, false, true);
    activity = getManager().getActivity(activityId);
    comments = getManager().getCommentsWithListAccess(activity).loadAsList(0, 10);
    // Number of comments must be 2
    assertEquals(2, comments.size());
    assertEquals("Has voted for " + options[1] + ".", comments.get(1).getTitle());

    // vote again poll
    String[] voteAgain = { "root:0" };
    pollTopic.setUserVote(voteAgain);
    pollTopic.setPollAction(PollAction.Vote_Again_Poll);
    pollService.savePoll(pollTopic, false, true);
    activity = getManager().getActivity(activityId);
    comments = getManager().getCommentsWithListAccess(activity).loadAsList(0, 10);
    // Number of comments must be 3
    assertEquals(3, comments.size());
    assertEquals("Has changed is voted for " + options[0] + ".", comments.get(2).getTitle());

    // delete activity
    getManager().deleteActivity(activity);

    // vote again
    String[] newVoteAgain = { "root:1" };
    pollTopic.setUserVote(newVoteAgain);
    pollTopic.setPollAction(PollAction.Vote_Again_Poll);
    pollService.savePoll(pollTopic, false, true);
    String newActivityId = pollService.getActivityIdForOwner(pollTopic.getParentPath() + "/"+ pollTopic.getId());
    activity = getManager().getActivity(newActivityId);
    comments = getManager().getCommentsWithListAccess(activity).loadAsList(0, 10);
    // Number of comments must be 0
    assertEquals(0, comments.size());

    // remove poll
    pollService.removePoll(pollTopic.getId());
  }

  private ActivityManager getManager() {
    return (ActivityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ActivityManager.class);
  }

}
