package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.webui.commons.UISpacesSwitcher;
import org.exoplatform.webui.config.annotation.ComponentConfig;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 7/21/15
 * #comments here
 */
@ComponentConfig(
        template =  "classpath:groovy/ecm/social-integration/share-document/UIShareDocumentSpaceMention.gtmpl"
)
public class UIShareDocumentSpaceMention extends UIContainer{

  public UIShareDocumentSpaceMention() throws Exception{
    addChild(UISpacesSwitcher.class, null, "SpaceSwitcher");
    getChild(UISpacesSwitcher.class).setShowPortalSpace(false);
    getChild(UISpacesSwitcher.class).setShowUserSpace(false);
  }
}
