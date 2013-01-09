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

import java.util.HashMap;
import java.util.Map;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Jan 9, 2013  
 */
public abstract class TopicActivityTask extends AbstractActivityTask<ForumActivityContext> {
  @Override
  public void start(ForumActivityContext ctx) {}
  
  @Override
  public void end(ForumActivityContext ctx) { }
  
  protected Map<String, String> makeParams(String id, String link, String owner, String name) throws Exception {
    Map<String, String> templateParams = new HashMap<String, String>();
    templateParams.put(TOPIC_ID_KEY, id);
    templateParams.put(TOPIC_LINK_KEY, link);
    templateParams.put(TOPIC_NAME_KEY, name);
    templateParams.put(TOPIC_OWNER_KEY, owner);
    return templateParams;
  }

  public static TopicActivityTask CREATE_TOPIC = new TopicActivityTask() {
    @Override
    public ExoSocialActivity publish(ForumActivityContext ctx) {
      return null;
    }
    
  };
  
  public static TopicActivityTask UPDATE_TOPIC = new TopicActivityTask() {

    @Override
    public ExoSocialActivity publish(ForumActivityContext ctx) {
      return null;
    }
    
  };
  
  public static TopicActivityTask MOVE_TOPIC = new TopicActivityTask() {

    @Override
    public ExoSocialActivity publish(ForumActivityContext ctx) {
      return null;
    }
  };
  
  public static TopicActivityTask UPDATE_STATUS_TOPIC = new TopicActivityTask() {

    @Override
    public ExoSocialActivity publish(ForumActivityContext ctx) {
      return null;
    }
  };
  
  public static TopicActivityTask MERGE_TOPIC = new TopicActivityTask() {

    @Override
    public ExoSocialActivity publish(ForumActivityContext ctx) {
      return null;
    }
  };
  
  public static TopicActivityTask SPLIT_TOPIC = new TopicActivityTask() {

    @Override
    public ExoSocialActivity publish(ForumActivityContext ctx) {
      return null;
    }
  };
}
