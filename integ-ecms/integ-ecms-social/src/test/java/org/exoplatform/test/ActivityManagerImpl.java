/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.test;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.ActivityProcessor;
import org.exoplatform.social.core.BaseActivityProcessorPlugin;
import org.exoplatform.social.core.activity.ActivityListenerPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.storage.ActivityStorageException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Dec 8, 2014  
 */
public class ActivityManagerImpl implements ActivityManager {
  public ActivityManagerImpl(){}
  @Override
  public void saveActivityNoReturn(Identity streamOwner, ExoSocialActivity activity) {
    // TODO Auto-generated method stub

  }

  @Override
  public void saveActivityNoReturn(ExoSocialActivity activity) {
    // TODO Auto-generated method stub

  }

  @Override
  public void saveActivity(Identity streamOwner, String type, String title) {
    // TODO Auto-generated method stub

  }

  @Override
  public ExoSocialActivity getActivity(String activityId) {

    return activitiesList.get(Integer.parseInt(activityId));
  }

  @Override
  public ExoSocialActivity getParentActivity(ExoSocialActivity comment) {
    // TODO Auto-generated method stub
    return null;
  }
  private static ArrayList<ExoSocialActivity> activitiesList = new ArrayList<ExoSocialActivity>();
  @Override
  public void updateActivity(ExoSocialActivity activity) {
    activity.setId(activitiesList.size() + "");
    activitiesList.add(activity);

  }

  @Override
  public void deleteActivity(ExoSocialActivity activity) {
    // TODO Auto-generated method stub

  }

  @Override
  public void deleteActivity(String activityId) {
    // TODO Auto-generated method stub

  }

  @Override
  public void saveComment(ExoSocialActivity activity, ExoSocialActivity newComment) {
    // TODO Auto-generated method stub

  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getCommentsWithListAccess(ExoSocialActivity activity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void deleteComment(String activityId, String commentId) {
    // TODO Auto-generated method stub

  }

  @Override
  public void deleteComment(ExoSocialActivity activity, ExoSocialActivity comment) {
    // TODO Auto-generated method stub

  }

  @Override
  public void saveLike(ExoSocialActivity activity, Identity identity) {
    // TODO Auto-generated method stub

  }

  @Override
  public void deleteLike(ExoSocialActivity activity, Identity identity) {
    // TODO Auto-generated method stub

  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesWithListAccess(Identity identity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesWithListAccess(Identity ownerIdentity,
                                                                           Identity viewerIdentity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesOfConnectionsWithListAccess(Identity identity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesOfSpaceWithListAccess(Identity spaceIdentity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesOfUserSpacesWithListAccess(Identity identity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivityFeedWithListAccess(Identity identity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesByPoster(Identity poster) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public RealtimeListAccess<ExoSocialActivity> getActivitiesByPoster(Identity posterIdentity,
                                                                     String... activityTypes) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void addProcessor(ActivityProcessor activityProcessor) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addProcessorPlugin(BaseActivityProcessorPlugin activityProcessorPlugin) {
    // TODO Auto-generated method stub

  }

  @Override
  public void addActivityEventListener(ActivityListenerPlugin activityListenerPlugin) {
    // TODO Auto-generated method stub

  }

  @Override
  public ExoSocialActivity saveActivity(Identity streamOwner, ExoSocialActivity activity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ExoSocialActivity saveActivity(ExoSocialActivity activity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivities(Identity identity) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivities(Identity identity, long start, long limit) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnections(Identity ownerIdentity) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfConnections(Identity ownerIdentity,
                                                            int offset,
                                                            int length) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivitiesOfUserSpaces(Identity ownerIdentity) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ExoSocialActivity> getActivityFeed(Identity identity) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void removeLike(ExoSocialActivity activity, Identity identity) throws ActivityStorageException {
    // TODO Auto-generated method stub

  }

  @Override
  public List<ExoSocialActivity> getComments(ExoSocialActivity activity) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ExoSocialActivity recordActivity(Identity owner, String type, String title) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ExoSocialActivity recordActivity(Identity owner, ExoSocialActivity activity) throws Exception {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ExoSocialActivity recordActivity(Identity owner, String type, String title, String body) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getActivitiesCount(Identity owner) throws ActivityStorageException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void processActivitiy(ExoSocialActivity activity) {
    // TODO Auto-generated method stub

  }

}
