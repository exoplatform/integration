/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.ext.common;

import java.util.List;
import java.util.Map;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.user.CommonContact;
import org.exoplatform.forum.common.user.ContactProvider;
import org.exoplatform.forum.common.user.DefaultContactProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.service.LinkProvider;

/**
 * Created by The eXo Platform SAS Author : Vu Duy Tu tu.duy@exoplatform.com Sep
 * 28, 2011
 */
public class SocialContactProvider implements ContactProvider {

  private static final Log LOG = ExoLogger.getLogger(SocialContactProvider.class);

  @SuppressWarnings("unchecked")
  @Override
  public CommonContact getCommonContact(String userId) {
    
    OrganizationService orgService = (OrganizationService) ExoContainerContext.getCurrentContainer()
                                                                              .getComponentInstanceOfType(OrganizationService.class);
    CommonContact defaultContact = new DefaultContactProvider(orgService).getCommonContact(userId);
    
    CommonContact contact = null;
    IdentityManager identityM = (IdentityManager) ExoContainerContext.getCurrentContainer()
                                                                     .getComponentInstanceOfType(IdentityManager.class);
    if (identityM != null) {
      Identity userIdentity = identityM.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                            userId,
                                                            true);
      if (userIdentity != null) {
        contact = new CommonContact();
        Profile profile = userIdentity.getProfile();

        contact.setEmailAddress(profile.getEmail());
        contact.setFullName(profile.getFullName());
        contact.setAvatarUrl(profile.getAvatarUrl());
        contact.setJob(profile.getPosition());
        if (profile.contains(Profile.FIRST_NAME)) {
          contact.setFirstName(profile.getProperty(Profile.FIRST_NAME).toString());
        }
        if (profile.contains(Profile.LAST_NAME)) {
          contact.setLastName(profile.getProperty(Profile.LAST_NAME).toString());
        }
        if (profile.contains(Profile.GENDER)) {
          contact.setGender(profile.getProperty(Profile.GENDER).toString());
        }
        if (profile.contains(Profile.CONTACT_PHONES)) {
          List<Map<String, String>> profiles = (List<Map<String, String>>) profile.getProperty(Profile.CONTACT_PHONES);
          if (profiles != null) {
            for (Map<String, String> mapInfo : profiles) {
              contact.setWorkPhone(getValueByKey(mapInfo, "Work", contact.getWorkPhone()));
              contact.setHomePhone(getValueByKey(mapInfo, "Home", contact.getHomePhone()));
            }
          }
        }
        if (profile.contains(Profile.CONTACT_URLS)) {
          List<Map<String, String>> profiles = (List<Map<String, String>>) profile.getProperty(Profile.CONTACT_URLS);
          if (profiles != null) {
            for (Map<String, String> mapInfo : profiles) {
              contact.setWebSite(getValueByKey(mapInfo, "url", contact.getWebSite()));
            }
          }
        } else {
          contact.setWebSite(LinkProvider.getProfileUri(userId));
        }
        if (CommonUtils.isEmpty(contact.getBirthday()))
          contact.setBirthday(defaultContact.getBirthday());
        if (CommonUtils.isEmpty(contact.getCountry()))
          contact.setCountry(defaultContact.getCountry());
        if (CommonUtils.isEmpty(contact.getCity()))
          contact.setCity(defaultContact.getCity());
        if (CommonUtils.isEmpty(contact.getHomePhone()))
          contact.setHomePhone(defaultContact.getHomePhone());
        if (CommonUtils.isEmpty(contact.getWorkPhone()))
          contact.setWorkPhone(defaultContact.getWorkPhone());
      }
    }
    if (contact == null) {
      LOG.warn(String.format("Could not retrieve forum user profile for %s by SocialContactProvider, DefaultContactProvider will be used.",
                             userId));
      return defaultContact;
    }
    return contact;
  }

  private String getValueByKey(Map<String, String> mapInfo, String key, String value) {
    if (mapInfo != null && key.equals(mapInfo.get("key"))) {
      String str = mapInfo.get("value");
      if (value.length() > 0) {
        str = new StringBuffer(value).append(", ").append(str).toString();
      }
      return str;
    }
    return value;
  }

}
