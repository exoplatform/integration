/*
 * Copyright (C) 2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.social.addons.rdbms.listener;

import java.net.URL;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.util.UriEncoder;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.upload.UploadService;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/test-root-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/jcr/jcr-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/jcr-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-portal-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/test-portal-configuration.xml") })
public class ActivityImageLinkUpdateListenerTest extends BaseCommonsTestCase {
  private static final Log        LOG = ExoLogger.getLogger(ActivityImageLinkUpdateListenerTest.class);

  private IdentityManager         identityManager;

  private ActivityManager         activityManager;

  private MockUploadService       uploadService;

  private List<ExoSocialActivity> tearDownActivityList;

  private Identity                rootIdentity;

  @Override
  protected void beforeRunBare() {
    // This is used to make a workaround for embedded file path
    // see
    // org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver.DMSMimeTypeResolver()
    System.setProperty("mock.portal.dir", System.getProperty("gatein.test.output.path") + "/test-classes");
    super.beforeRunBare();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    identityManager = getContainer().getComponentInstanceOfType(IdentityManager.class);
    activityManager = getContainer().getComponentInstanceOfType(ActivityManager.class);
    uploadService = (MockUploadService) getContainer().getComponentInstanceOfType(UploadService.class);
    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    tearDownActivityList = new ArrayList<ExoSocialActivity>();
    Authenticator authenticator = getContainer().getComponentInstanceOfType(Authenticator.class);
    org.exoplatform.services.security.Identity identity = authenticator.createIdentity("root");
    ConversationState conversationState = new ConversationState(identity);
    ConversationState.setCurrent(conversationState);
    IdentityRegistry identityRegistry = getContainer().getComponentInstanceOfType(IdentityRegistry.class);
    identityRegistry.register(identity);
    ExoContainerContext.setCurrentContainer(getContainer());
    SessionProviderService sessionProviderService = getContainer().getComponentInstanceOfType(SessionProviderService.class);
    sessionProviderService.setSessionProvider(null, new SessionProvider(conversationState));
  }

  @Override
  protected void tearDown() throws Exception {
    for (ExoSocialActivity activity : tearDownActivityList) {
      try {
        activityManager.deleteActivity(activity.getId());
      } catch (Exception e) {
        LOG.warn("can not delete activity with id: " + activity.getId());
      }
    }
    super.tearDown();
  }

  public void testActivityCreateWithUploadedImage() {
    String uploadId = String.valueOf((long) (Math.random() * 100000L));
    String body = "<img src='http://localhost:8080/test?uploadId=" + uploadId + "'>";

    ExoSocialActivity activity = createActivityWithBody(body, uploadId);

    assertEquals("<img src='http://localhost:8080"+UriEncoder.encode("/portal/rest/jcr/repository/portal-test/Users/r___/ro___/roo___/root/Private/Public/Activity Stream Documents/Pictures/"
        + YearMonth.now().getYear() + "/" + YearMonth.now().getMonthValue() + "/fileName.xml")+"'>", activity.getBody());
    activity = activityManager.getActivity(activity.getId());
    assertEquals("<img src='http://localhost:8080"+UriEncoder.encode("/portal/rest/jcr/repository/portal-test/Users/r___/ro___/roo___/root/Private/Public/Activity Stream Documents/Pictures/"
        + YearMonth.now().getYear() + "/" + YearMonth.now().getMonthValue() + "/fileName.xml")+"'>", activity.getBody());
    assertEquals(0, uploadService.getUploadResources().size());

    uploadId = String.valueOf((long) (Math.random() * 100000L));
    body = "<img src='/test?uploadId=" + uploadId + "'>";

    activity = createActivityWithBody(body, uploadId);

    assertEquals("<img src='http://localhost:8080"+UriEncoder.encode("/portal/rest/jcr/repository/portal-test/Users/r___/ro___/roo___/root/Private/Public/Activity Stream Documents/Pictures/"
        + YearMonth.now().getYear() + "/" + YearMonth.now().getMonthValue() + "/fileName(1).xml")+"'>", activity.getBody());
    activity = activityManager.getActivity(activity.getId());
    assertEquals("<img src='http://localhost:8080"+UriEncoder.encode("/portal/rest/jcr/repository/portal-test/Users/r___/ro___/roo___/root/Private/Public/Activity Stream Documents/Pictures/"
        + YearMonth.now().getYear() + "/" + YearMonth.now().getMonthValue() + "/fileName(1)") + ".xml'>", activity.getBody());
    assertEquals(0, uploadService.getUploadResources().size());
  }

  private ExoSocialActivity createActivityWithBody(String body, String uploadId) {
    URL resource = getClass().getResource("/test.xml");

    try {
      uploadService.createUploadResource(uploadId, resource.getPath(), "fileName.xml", "text/xml");
    } catch (Exception e) {
      fail("Error uploading file", e);
    }

    ExoSocialActivity activity = createActivity(rootIdentity, body);
    end();
    begin();

    assertNotNull(activity);
    assertNotNull(activity.getId());
    return activity;
  }

  /**
   * Creates activity on user's stream.
   * 
   * @param user
   * @return
   */
  private ExoSocialActivity createActivity(Identity user, String body) {
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("title");
    activity.setBody(body);
    activity.setPosterId(user.getId());
    activity.setUserId(user.getId());
    try {
      activityManager.saveActivityNoReturn(user, activity);
      tearDownActivityList.add(activity);
    } catch (Exception e) {
      LOG.error("can not save activity.", e);
    }
    return activity;
  }

}
