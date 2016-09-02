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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;


import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.notification.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIContainer;



/**
 * Created by The eXo Platform SAS
 * Author : Walid Khessairi
 *          wkhessairi@exoplatform.com
 * Aug 11, 2016
 */
@ComponentConfig(
    template =  "classpath:groovy/ecm/social-integration/share-document/UIWhoHasAccess.gtmpl"
)
public class UIWhoHasAccess extends UIContainer {

    private static final Log    LOG                 = ExoLogger.getLogger(UIWhoHasAccess.class);
    private static final String SPACE_PREFIX1 = "space::";
    private static final String SPACE_PREFIX2 = "*:/spaces/";

    public void close() {
        for(UIComponent uicomp : getChildren()) {
            removeChild(UIWhoHasAccessEntry.class);
        }
    }

    public UIWhoHasAccess()  {
    }
    public void init() {
        try {
            UIShareDocuments uishareDocuments = getAncestorOfType(UIShareDocuments.class);
            for (String id : uishareDocuments.getAllPermissions().keySet()) {
                if (getChildById(id) == null) addChild(UIWhoHasAccessEntry.class, null, id);
                UIWhoHasAccessEntry uiWhoHasAccessEntry = getChildById(id);
                uiWhoHasAccessEntry.init(id, uishareDocuments.getPermission(id));
            }

        } catch (Exception e) {
            if (LOG.isErrorEnabled())
                LOG.error(e.getMessage(), e);
        }
    }

    public void update(String name, String permission) {
        try {
            if (getChildById(name) == null) addChild(UIWhoHasAccessEntry.class, null, name);
            UIWhoHasAccessEntry uiWhoHasAccessEntry = getChildById(name);
            uiWhoHasAccessEntry.init(name, permission);
        } catch (Exception e) {
            if(LOG.isErrorEnabled())
                LOG.error(e.getMessage(), e);
        }
    }

    public void removeEntry(String id) {
        try {
            removeChildById(id);
            UIShareDocuments uiShareDocuments = getParent();
            uiShareDocuments.removePermission(id);
        } catch (Exception e) {
            if(LOG.isErrorEnabled())
                LOG.error(e.getMessage(), e);
        }
    }

    public void updateEntry(String id, String permission) {
        try {
            UIShareDocuments uiShareDocuments = getParent();
            uiShareDocuments.updatePermission(id, permission);
        } catch (Exception e) {
            if(LOG.isErrorEnabled())
                LOG.error(e.getMessage(), e);
        }
    }
    public String getAvatar(String name) {
        if (!isSpace(name)) {
            Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, name, true);
            Profile profile = identity.getProfile();
            return profile.getAvatarUrl() != null ? profile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL;
        } else {
            SpaceService spaceService = getApplicationComponent(SpaceService.class);
            Space space;
            if (name.startsWith(SPACE_PREFIX1)) space = spaceService.getSpaceByPrettyName(name.substring(SPACE_PREFIX1.length()));
            else space = spaceService.getSpaceByPrettyName(name.substring(SPACE_PREFIX2.length()));
            return space.getAvatarUrl() != null ? space.getAvatarUrl() : LinkProvider.SPACE_DEFAULT_AVATAR_URL;
        }
    }

    public String getProfileUrl(String name) {
        return CommonsUtils.getCurrentDomain() + LinkProvider.getProfileUri(name);
    }

    private boolean isSpace(String name) {
        return (name.startsWith(SPACE_PREFIX1) || name.startsWith(SPACE_PREFIX2));
    }

    public String getFullName(String name) {
        try {
            return getApplicationComponent(OrganizationService.class).getUserHandler().findUserByName(name).getDisplayName();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return "";
    }

    public String getUserName(String name) {
        try {
            return getApplicationComponent(OrganizationService.class).getUserHandler().findUserByName(name).getUserName();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return "";
    }
}