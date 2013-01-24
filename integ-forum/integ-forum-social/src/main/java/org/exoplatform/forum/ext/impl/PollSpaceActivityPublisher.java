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
package org.exoplatform.forum.ext.impl;

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.poll.service.Poll;
import org.exoplatform.poll.service.Poll.PollAction;
import org.exoplatform.poll.service.PollEventListener;
import org.exoplatform.poll.service.PollService;
import org.exoplatform.poll.service.Utils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.BaseActivityProcessorPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;

public class PollSpaceActivityPublisher extends PollEventListener{

  public static final String POLL_APP_ID          = "ks-poll:spaces";
  
  public static final String POLL_COMMENT_APP_ID  = "poll:spaces";
  
  private static final Log   LOG                  = ExoLogger.getExoLogger(PollSpaceActivityPublisher.class);

  public static final String POLL_LINK_KEY        = "PollLink";
  
  public static final String VOTE_POLL_TITLE_ID   = "vote_poll";
  
  public static final String VOTE_AGAIN_POLL_TITLE_ID = "vote_again_poll";
  
  public static final String VOTE_VALUE           = "VOTE_VALUE";
  
  private ExoSocialActivity activity(Identity author, String title, String body, Map<String, String> templateParams) throws Exception {
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setUserId(author.getId());
    activity.setTitle(title);
    activity.setBody(body);
    activity.setType(POLL_APP_ID);
    activity.setTemplateParams(templateParams);
    return activity;
  }
  
  private void savePollForActivity(Poll poll) {
    PollService pollService = (PollService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(PollService.class);
    IdentityManager identityManager = (IdentityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(IdentityManager.class);
    try {
      Identity pollOwnerIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, poll.getOwner(), false);
      Map<String, String> templateParams = new HashMap<String, String>();
      ExoSocialActivity activity = activity(pollOwnerIdentity, poll.getQuestion(), Utils.getInfoVote(poll), templateParams);
      String pollPath = poll.getParentPath()+"/"+poll.getId();
      String activityId = pollService.getActivityIdForOwner(pollPath);
      if (activityId != null) {
        ExoSocialActivity got = getManager().getActivity(activityId);
        if (got != null) {
          activity = got;
          poll.setInfoVote();
          activity.setBody(Utils.getInfoVote(poll));
          activity.setTitle(poll.getQuestion());
          ExoSocialActivityImpl comment = new ExoSocialActivityImpl();
          String currentName = Utils.getCurrentUserVote(poll);
          if (currentName.equals(poll.getOwner())) {
            comment.setUserId(pollOwnerIdentity.getId());
            comment.setTitle(poll.getPollAction().getMessage(Utils.getUserVote(poll, poll.getOwner())));
          } else {
            Identity currentIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, currentName, false);
            comment.setUserId(currentIdentity.getId());
            comment.setTitle(poll.getPollAction().getMessage(Utils.getUserVote(poll,currentName)));
          }
          if (poll.getPollAction().equals(PollAction.Vote_Poll)) {
            comment.setTitleId(VOTE_POLL_TITLE_ID);
          } 
          if (poll.getPollAction().equals(PollAction.Vote_Again_Poll)) {
            comment.setTitleId(VOTE_AGAIN_POLL_TITLE_ID);
          }
          templateParams.put(VOTE_VALUE, Utils.getUserVote(poll, poll.getOwner()));
          templateParams.put(BaseActivityProcessorPlugin.TEMPLATE_PARAM_TO_PROCESS, VOTE_VALUE);
          comment.setType(POLL_COMMENT_APP_ID);
          comment.setTemplateParams(templateParams);
          getManager().updateActivity(activity);
          getManager().saveComment(activity, comment);
        } else {
          activityId = null;
          poll.setInfoVote();
          activity.setBody(Utils.getInfoVote(poll));
        }
      }
      if (activityId == null) {
        templateParams.put(POLL_LINK_KEY, poll.getLink());
        activity.setTemplateParams(templateParams);
        getManager().saveActivityNoReturn(pollOwnerIdentity, activity);
        pollService.saveActivityIdForOwner(pollPath, activity.getId());
      }
    } catch (Exception e) {
      LOG.error("Can not record Activity for space when create poll ", e.getMessage());
    }
  }
  
  public void savePoll(Poll poll) {
    savePollForActivity(poll);
  }
  
  public void pollRemove(String pollId) {
    try {
      PollService pollService = (PollService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(PollService.class);
      Poll poll = pollService.getPoll(pollId);
      String pollPath = poll.getParentPath()+"/"+poll.getId();
      String activityId = pollService.getActivityIdForOwner(pollPath);
      ExoSocialActivity activity = getManager().getActivity(activityId);
      getManager().deleteActivity(activity);
    } catch (Exception e) {
      LOG.error("Fail to remove poll "+e.getMessage());
    }
  }
  
  private ActivityManager getManager() {
    return (ActivityManager) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ActivityManager.class);
  }
  
}
