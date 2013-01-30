package org.exoplatform.wcm.ext.component.activity.listener;
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
import javax.jcr.Node;

import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

/**
 * Created by The eXo Platform SAS
 * Author : Nguyen The Vinh From ECM Of eXoPlatform
 *          vinh_nguyen@exoplatform.com
 * Handler Comment Added event
 * 16 Jan 2013  
 */
public class CommentAddedActivityListener extends Listener<Node, Node> {
  @Override
  public void onEvent(Event<Node, Node> event) throws Exception {
    Node currentNode = event.getSource();
    Node commentNode = event.getData();
    String commentContent = "";
    if (commentNode.hasProperty("exo:commentContent")) {
      try {
        commentContent = commentNode.getProperty("exo:commentContent").getValue().getString();
      }catch (Exception e) {
        commentContent =null;
      }
    }
    if (commentContent==null) return;
    ExoSocialActivity commentActivity = Utils.postActivity(currentNode, "{0}", false, true, commentContent);
    if (commentActivity!=null) {
      ActivityTypeUtils.attachActivityId(commentNode, commentActivity.getId());
      commentNode.save();
      commentNode.getSession().save();
    }
  }
}
