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
package org.exoplatform.wcm.ext.component.activity.listener;
import javax.jcr.Node;

import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

public class TagActivityListener extends Listener<Node, String>{
  
  private static String TAG_ADDED_BUNDLE        = "SocialIntegration.messages.tagAdded";
  private static String TAG_REMOVED_BUNDLE      = "SocialIntegration.messages.tagRemoved";
  
  @Override
  public void onEvent(Event<Node, String> event) throws Exception {
    String eventName = event.getEventName();
    Node currentNode = event.getSource();
    String tagValue = event.getData();
    String bundleMessage = ActivityCommonService.TAG_ADDED_ACTIVITY.equals(eventName)?TAG_ADDED_BUNDLE:TAG_REMOVED_BUNDLE;
    if (eventName.equals(ActivityCommonService.CATEGORY_ADDED_ACTIVITY)) {
      Utils.postActivity(currentNode, bundleMessage, false, true, tagValue);
    }
    
  }

}
