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

import org.exoplatform.social.core.activity.model.ExoSocialActivity;

/**
 * Created by The eXo Platform SAS Author : thanh_vucong
 * thanh_vucong@exoplatform.com Jan 9, 2013
 */
public enum ForumActivityType {

  ADD_TOPIC("%s"),
  UPDATE_TOPIC_TITLE("Title has been updated to: %s"),
  UPDATE_TOPIC_CONTENT("Content has been edited."),
  CLOSE_TOPIC("Topic has been closed."),
  OPEN_TOPIC("Topic has been opened."),
  LOCK_TOPIC("Topic has been locked."),
  UNLOCK_TOPIC("Topic has been unlocked."),
  MERGE_TOPICS("%s"),
  SPLIT_TOPIC("%s"),
  MOVE_TOPIC("Topic have been moved to: %s>%s"),
  RATE_TOPIC("Rated the topic: %s"),
  ADD_POST(""),
  UPDATE_POST("Edited his reply to: %s");

  private final String titleTemplate;

  private ForumActivityType(String titleTemplate) {
    this.titleTemplate = titleTemplate;
  }
  
  public ExoSocialActivity getActivity(ExoSocialActivity a, String value) {
    if (value != null) {
      a.setTitle(String.format(titleTemplate, value));
    }
    
    return a;
  }
  
  public ExoSocialActivity getActivity(ExoSocialActivity a, String value1, String value2) {
    if (value1 != null && value2 != null) {
      a.setTitle(String.format(titleTemplate, value1, value2));
    }
    
    return a;
  }
}
