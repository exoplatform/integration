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
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * Handler for Content's Revision changed event
 * 21 Jan 2013  
 */
public class ContentRevisionActivityListener extends Listener<Node, String>{
  private static String REVISION_CHANGED_BUNDLE        = "SocialIntegration.messages.revisionChanged";
  @Override
  public void onEvent(Event<Node, String> event) throws Exception {
    Node currentNode = event.getSource();
    String versionName = event.getData();
    Utils.postActivity(currentNode, REVISION_CHANGED_BUNDLE, true, true, versionName, "");
  }
}
