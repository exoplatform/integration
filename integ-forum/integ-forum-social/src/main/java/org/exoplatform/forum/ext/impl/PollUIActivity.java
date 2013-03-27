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

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.web.url.navigation.NavigationResource;
import org.exoplatform.web.url.navigation.NodeURL;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;

@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "classpath:groovy/forum/social-integration/plugin/space/PollUIActivity.gtmpl", events = {
  @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
  @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
  @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
  @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
  @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
  @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
  @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class) })
public class PollUIActivity extends BaseKSActivity {

  @SuppressWarnings("unused")
  private String[] getVotes(String infoVote) {
    String[] tab = infoVote.split("\\|");
    tab = (String[]) ArrayUtils.removeElement(tab, tab[tab.length-1]);
    for (int i = 0; i< tab.length; i++) {
      String option = tab[i].split(":")[0];
      String percent = tab[i].split(":")[1];
      String nbVotes = tab[i].split(":")[2];
      int number = Integer.parseInt(nbVotes);
      if (number <= 1) {
        nbVotes = WebUIUtils.getLabel(null, "PollUIActivity.label.vote").replace("{0}", String.valueOf(number));
      } else {
        nbVotes = WebUIUtils.getLabel(null, "PollUIActivity.label.votes").replace("{0}", String.valueOf(number));
      }
      StringBuilder sb = new StringBuilder();
      sb.append(CommonUtils.decodeSpecialCharToHTMLnumber(option)).append(":").append(percent).append(":").append(nbVotes);
      tab[i] = sb.toString();
    }
    return tab;
  }
  
  @SuppressWarnings("unused")
  private String getNumberOfVotes(String infoVote) {
    String[] tab = infoVote.split("\\|");
    int number = Integer.parseInt(tab[tab.length-1]);
    if (number <= 1) {
      return WebUIUtils.getLabel(null, "PollUIActivity.label.vote").replace("{0}", String.valueOf(number));
    } else {
      return WebUIUtils.getLabel(null, "PollUIActivity.label.votes").replace("{0}", String.valueOf(number));
    }
  }
  
  @SuppressWarnings("unused")
  private String getLink() {
    String spaceLink = getSpaceHomeURL(getSpaceGroupId());
    if (spaceLink == null)
      return getActivityParamValue(PollSpaceActivityPublisher.POLL_LINK_KEY);
    String topicId = getActivityParamValue(PollSpaceActivityPublisher.POLL_ID);
    String topicLink = String.format("%s/forum/topic/%s", spaceLink, topicId);
    return topicLink;
  }
  
  private String getSpaceGroupId() {
    return getActivityParamValue(PollSpaceActivityPublisher.SPACE_GROUP_ID);
  }
  
  public String getSpaceHomeURL(String spaceGroupId) {
    if ("".equals(spaceGroupId))
      return null;
    String permanentSpaceName = spaceGroupId.split("/")[2];
    SpaceService spaceService  = (SpaceService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);
    Space space = spaceService.getSpaceByGroupId(spaceGroupId);
    
    NodeURL nodeURL =  RequestContext.getCurrentInstance().createURL(NodeURL.TYPE);
    NavigationResource resource = new NavigationResource(SiteType.GROUP, SpaceUtils.SPACE_GROUP + "/"
                                        + permanentSpaceName, space.getPrettyName());
   
    return nodeURL.setResource(resource).toString(); 
  }
  
}
