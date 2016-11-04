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

import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.service.LinkProvider;

import javax.jcr.Node;
import java.util.List;

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
        commentContent = processMentions(commentContent);
        commentNode.setProperty("exo:commentContent", commentContent);
        commentNode.save();

      }catch (Exception e) {
        commentContent =null;
      }
    }
    if (commentContent==null) return;
    ExoSocialActivity commentActivity;
    if(currentNode.isNodeType(NodetypeConstant.NT_FILE)) {
      commentActivity = Utils.postFileActivity(currentNode, "{0}", false, true, commentContent, "");
    }else{
      commentActivity= Utils.postActivity(currentNode, "{0}", false, true, commentContent, "");
    }
    LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
    List<Node> links = linkManager.getAllLinks(currentNode, NodetypeConstant.EXO_SYMLINK);

    for(Node link: links){
      if(link.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)){
        ExoSocialActivity linkCommentActivity = Utils.postActivity(link, "{0}", false, true, commentContent, "");
        if (commentActivity!=null) {
          ActivityTypeUtils.attachActivityId(link, linkCommentActivity.getId());
        }
      }
    }
    if (commentActivity!=null) {    	
      ActivityTypeUtils.attachActivityId(commentNode, commentActivity.getId());
      commentNode.getSession().save();
    }
  }

  private String processMentions(String comment) {
    String excerpts[] = comment.split("&#64;");
    comment = excerpts[0];
    String mentioned = "";
    for (int i=1; i<excerpts.length; i++) {
      String name = excerpts[i].split(" ")[0];
      Identity identity = org.exoplatform.social.notification.Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, name, true);
      if (identity != null) {
        mentioned = addMentioned(name, identity.getProfile().getFullName());
      }
      if (mentioned.isEmpty()) {
        if (excerpts[i].isEmpty()) comment = comment + " ";
        else comment = comment + excerpts[i] + " ";
      } else {
        comment = comment + mentioned + excerpts[i].substring(name.length(),excerpts[i].length());
        mentioned = "";
      }
    }
    return comment;
  }

  private String addMentioned(String mention, String fullname) {
    String profileURL = CommonsUtils.getCurrentDomain() + LinkProvider.getProfileUri(mention);
    return "<a href=" + profileURL + " rel=\"nofollow\">" + fullname + "</a>";
  }
}
