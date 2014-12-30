/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.ext.component.activity;

import org.exoplatform.social.plugin.link.UILinkUtil;
import org.exoplatform.social.service.rest.Util;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.social.webui.activity.UIActivitiesContainer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
        lifecycle = UIFormLifecycle.class,
        template = "classpath:groovy/ecm/social-integration/plugin/link/UILinkActivity.gtmpl",
        events = {
                @EventConfig(listeners = UILinkActivity.ViewDocumentActionListener.class),
                @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
                @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
                @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
                @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
                @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
                @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
                @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class)
        }
)
public class UILinkActivity extends org.exoplatform.social.plugin.link.UILinkActivity {

  public void showDocumentPreview() throws Exception {
    UIActivitiesContainer uiActivitiesContainer = this.getParent();
    UIDocumentPreview uiDocumentPreview = uiActivitiesContainer.findComponentById("UIDocumentPreview");
    if (uiDocumentPreview == null) {
      uiDocumentPreview = uiActivitiesContainer.addChild(UIDocumentPreview.class, null, "UIDocumentPreview");
    }
    uiDocumentPreview.setBaseUIActivity(this);
    uiDocumentPreview.setRendered(true);
  }

  public static class ViewDocumentActionListener extends EventListener<UILinkActivity> {
    @Override
    public void execute(Event<UILinkActivity> event) throws Exception {
      UILinkActivity uiLinkActivity = event.getSource();

      uiLinkActivity.showDocumentPreview();

      UIActivitiesContainer activitiesContainer = uiLinkActivity.getParent();
      event.getRequestContext().addUIComponentToUpdateByAjax(activitiesContainer);
    }
  }
}
