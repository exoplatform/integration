/*
 * Copyright (C) 2003-2014 eXo Platform SEA.
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

package org.exoplatform.ecm.webui.component.explorer.popup.service;

import junit.framework.TestCase;

import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
//import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.ext.component.document.service.IShareDocumentService;
import org.exoplatform.wcm.ext.component.document.service.ShareDocumentService;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * Aug 7, 2014
 */
public class TestService extends TestCase {
  //private Log log = ExoLogger.getExoLogger(TestService.class);

  protected final String REPO_NAME = "repository";
  protected final String COLLABORATION_WS = "collaboration";

  private static StandaloneContainer container;
  private RepositoryService      repositoryService;
  protected ManageableRepository repository;
  protected SessionProvider sessionProvider;
  protected CredentialsImpl credentials;
  protected Session                session;
  protected SessionProviderService sessionProviderService_;

  private String perm = PermissionType.READ+","+PermissionType.ADD_NODE+","+PermissionType.SET_PROPERTY;
  private String comment = "Comment";
  private String spaceName = "/spaces/space1";
  private String nodePath = "nodeToShare";
  private String activityId;
  private NodeLocation nodeLocation;
  private String spaceId;

  private LinkManager linkManager =(LinkManager) container.getComponentInstanceOfType(LinkManager.class);

  

  static {
    initContainer();
  }

  /**
   * Set current container
   */
  private void begin() {
    RequestLifeCycle.begin(container);
  }

  /**
   * Clear current container
   */
  protected void tearDown() throws Exception {
    //delete node
    this.session.getRootNode().getNode(nodePath).remove();
    this.session.save();
    //delete activity
    ActivityManager manager = (ActivityManager)container.getComponentInstanceOfType(ActivityManager.class);
    manager.deleteActivity(activityId);
    //delete space
    SpaceService spaceService = (SpaceService) container.getComponentInstanceOfType(SpaceService.class);
    spaceService.deleteSpace(spaceId);
    RequestLifeCycle.end();
    System.out.println("TearDown complete!");
  }

  private static void initContainer() {
    try {
      String containerConf = Thread.currentThread()
              .getContextClassLoader()
              .getResource("conf/standalone/configuration.xml")
              .toString();
      StandaloneContainer.addConfigurationURL(containerConf);
      String loginConf = Thread.currentThread().getContextClassLoader().getResource("conf/standalone/login.conf").toString();
      System.setProperty("java.security.auth.login.config", loginConf);
      container = StandaloneContainer.getInstance();

    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize standalone container: " + e.getMessage(), e);
    }
  }

  @Override
  protected void setUp() throws Exception {
    begin();
    Identity systemIdentity = new Identity(IdentityConstants.SYSTEM);
    ConversationState.setCurrent(new ConversationState(systemIdentity));
    repositoryService = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class);
    sessionProviderService_ = (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class);
    //linkManager = (LinkManager)container.getComponentInstanceOfType(LinkManager.class);
    applySystemSession();
    
    //init Node to share
    this.nodeLocation = NodeLocation.getNodeLocationByNode(this.session.getRootNode().addNode(nodePath));
    session.save();
    //init space to share
    SpaceService spaceService = (SpaceService)container.getComponentInstanceOfType(SpaceService.class);
    //container.getComponentInstances();
    Space sp = new Space();
    sp.setId(spaceName);
    spaceId = spaceService.createSpace(sp, "").getId();
    //init space node
    Node group = null;
    if(!session.getRootNode().hasNode("Groups"))group = session.getRootNode().addNode("Groups");
    else group = session.getRootNode().getNode("Groups");
    Node spaces = null;
    if(!group.hasNode("spaces"))spaces = group.addNode("spaces");
    else spaces = group.getNode("spaces");
    Node space = spaces.addNode(spaceName.split("/")[2]);
    space.addNode("Documents");
    session.save();
    
    System.out.println("Setup complete!!!");
  }

  public void testShare() throws Exception {
  //share node
    IShareDocumentService temp = (IShareDocumentService) container.getComponentInstanceOfType(IShareDocumentService.class);
    activityId = temp.publicDocumentToSpace(spaceName, NodeLocation.getNodeByLocation(nodeLocation), comment, perm);


    //Test symbolic link
    NodeIterator nodeIterator = session.getRootNode().getNode("Groups").getNode("spaces").getNode(this.spaceName.split("/")[2]).getNode("Documents").getNode("Shared").getNodes();
    assertEquals(1, nodeIterator.getSize());
    Node target = nodeIterator.nextNode();
    assertEquals("exo:symlink", target.getPrimaryNodeType().getName());
    Node origin = linkManager.getTarget(target,true);
    assertEquals("/" + nodePath, origin.getPath());
    //Test permission
    ExtendedNode extendedNode = (ExtendedNode) origin;
    assertTrue(!extendedNode.getACL().getPermissions("*:" + spaceName).isEmpty());
    //Test activity
    ActivityManager manager = (ActivityManager) container.getComponentInstanceOfType(ActivityManager.class);
    ExoSocialActivity activity = manager.getActivity(this.activityId);
    assertEquals(this.comment, activity.getTemplateParams().get(ShareDocumentService.MESSAGE));
  }

  public void applySystemSession() throws Exception{
    System.setProperty("gatein.tenant.repository.name", REPO_NAME);
    container = StandaloneContainer.getInstance();

    repositoryService.setCurrentRepositoryName(REPO_NAME);
    repository = repositoryService.getCurrentRepository();

    closeOldSession();
    sessionProvider = sessionProviderService_.getSystemSessionProvider(null);
    session = sessionProvider.getSession(COLLABORATION_WS, repository);
    sessionProvider.setCurrentRepository(repository);
    sessionProvider.setCurrentWorkspace(COLLABORATION_WS);
  }

  /**
   * Close current session
   */
  private void closeOldSession() {
    if (session != null && session.isLive()) {
      session.logout();
    }
  }

  
}
