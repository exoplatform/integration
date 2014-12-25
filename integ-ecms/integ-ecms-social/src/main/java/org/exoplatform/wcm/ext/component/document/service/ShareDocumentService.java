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
package org.exoplatform.wcm.ext.component.document.service;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.picocontainer.Startable;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 19, 2014  
 */
public class ShareDocumentService implements IShareDocumentService, Startable{
  private static final Log    LOG                 = ExoLogger.getLogger(ShareDocumentService.class);
  public static final String SHARE_FILE = "sharefiles:spaces";
  public static final String SHARE_CONTENT = "sharecontents:spaces";
  public static final String MESSAGE            = "message";
  public static final String REPOSITORY         = "repository";
  public static final String WORKSPACE          = "WorkspaceName";
  public static final String MIME_TYPE          = "mimeType";
  public static final String NODE_PATH          = "NodePath";
  public static final String MIX_PRIVILEGEABLE          = "exo:privilegeable";
  private RepositoryService repoService;
  private SessionProviderService sessionProviderService;
  private LinkManager linkManager;
  public ShareDocumentService(RepositoryService _repoService,
                              LinkManager _linkManager,
                              //IdentityManager _identityManager,
                              SessionProviderService _sessionProviderService){
    this.repoService = _repoService;
    this.sessionProviderService = _sessionProviderService;
    this.linkManager = _linkManager;

  }

  /* (non-Javadoc)
   * @see org.exoplatform.ecm.webui.component.explorer.popup.service.IShareDocumentService#publicDocumentToSpace(java.lang.String, javax.jcr.Node, java.lang.String, java.lang.String)
   */
  @Override
  public String publicDocumentToSpace(String space, Node node, String comment,String perm) {
    Node rootSpace = null;
    Node shared = null;
    try {
      SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
      ManageableRepository repository = repoService.getCurrentRepository();
      Session session = sessionProvider.getSession(repository.getConfiguration().getDefaultWorkspaceName(), repository);
      //add symlink to destination space
      NodeHierarchyCreator nodeCreator = WCMCoreUtils.getService(NodeHierarchyCreator.class);
      nodeCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);

      rootSpace = (Node) session.getItem(nodeCreator.getJcrPath(BasePath.CMS_GROUPS_PATH) + space);
      rootSpace = rootSpace.getNode("Documents");
      if(!rootSpace.hasNode("Shared")){
        shared = rootSpace.addNode("Shared");
      }else{
        shared = rootSpace.getNode("Shared");
      }
      Node link = linkManager.createLink(shared, node);
      rootSpace.save();
      //Update permission 
      String tempPerms = perm.toString();//Avoid ref back to UIFormSelectBox options
      if(!tempPerms.equals(PermissionType.READ)) tempPerms = PermissionType.READ+","+PermissionType.ADD_NODE+","+PermissionType.SET_PROPERTY;
      if(canChangePermission(node)){
        node.addMixin(MIX_PRIVILEGEABLE);
        ExtendedNode enode = (ExtendedNode) node;
        enode.setPermission("*:" + space,tempPerms.split(","));
        enode.save();
      }
      //Share activity
      try {
        ExoSocialActivity activity = null;
        if(node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)){
          activity = org.exoplatform.wcm.ext.component.activity.listener.Utils.postFileActivity(link,"",false,false,comment);
          activity.setType(SHARE_FILE);
        }else{
          activity = org.exoplatform.wcm.ext.component.activity.listener.Utils.postActivity(link,"",false,false,comment);
          activity.setType(SHARE_CONTENT);
        }

        activity.getTemplateParams().put(NODE_PATH, link.getPath());        
        activity.getTemplateParams().put(MIME_TYPE , getMimeType(node));
        activity.getTemplateParams().put(MESSAGE, comment);
        activity.getTemplateParams().put(WORKSPACE, link.getSession().getWorkspace().getName());

        ActivityManager activityManager = WCMCoreUtils.getService(ActivityManager.class);
        activityManager.updateActivity(activity);
        return activity.getId();
      } catch (Exception e1) {
        if(LOG.isErrorEnabled())
          LOG.error(e1.getMessage(), e1);
      }
    } catch (RepositoryException e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    } catch (Exception e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
    return "";
  }
  private boolean canChangePermission(Node node) {
    try {
      ((ExtendedNode)node).checkPermission(PermissionType.CHANGE_PERMISSION);
      return true;
    }catch (RepositoryException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return false;
  }

  private String getMimeType(Node node) {
    try {
      if (node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
        if (node.hasNode(NodetypeConstant.JCR_CONTENT))
          return node.getNode(NodetypeConstant.JCR_CONTENT)
              .getProperty(NodetypeConstant.JCR_MIME_TYPE)
              .getString();
      }
    } catch (RepositoryException e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
    return "";
  }

  @Override
  public void start() {
    // TODO Auto-generated method stub

  }

  @Override
  public void stop() {
    // TODO Auto-generated method stub

  }
}
