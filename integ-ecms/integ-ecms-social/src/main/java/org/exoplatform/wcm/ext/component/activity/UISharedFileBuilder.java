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
package org.exoplatform.wcm.ext.component.activity;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.social.webui.activity.BaseUIActivityBuilder;
import org.exoplatform.wcm.ext.component.document.service.ShareDocumentService;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 25, 2014  
 */
public class UISharedFileBuilder extends BaseUIActivityBuilder {
  private static final Log LOG = ExoLogger.getLogger(FileUIActivityBuilder.class);

  @Override
  protected void extendUIActivity(BaseUIActivity uiActivity, ExoSocialActivity activity) {
    SharedFileUIActivity fileActivity = (SharedFileUIActivity) uiActivity;
    String nodePath = "";
    String workspaceName = "";
    //set data into the UI component of activity
    if (activity.getTemplateParams() != null) {
      fileActivity.setUIActivityData(activity.getTemplateParams());
      nodePath = activity.getTemplateParams().get(ShareDocumentService.NODE_PATH);
      workspaceName = activity.getTemplateParams().get(ShareDocumentService.WORKSPACE);
    }
    //get node data
    Node contentNode = null;
    try {
      ManageableRepository manageRepo = WCMCoreUtils.getRepository();
      if(workspaceName.equals("")) workspaceName = manageRepo.getConfiguration().getDefaultWorkspaceName();
      SessionProvider sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      LinkManager linkManager = PortalContainer.getInstance().getComponentInstanceOfType(LinkManager.class);
      contentNode = linkManager.getTarget((Node) sessionProvider.getSession(workspaceName, manageRepo).getItem(nodePath));
      fileActivity.docPath = contentNode.getPath();
      fileActivity.workspace = workspaceName;
      fileActivity.repository = manageRepo.toString();
      fileActivity.setActivityTitle(activity.getTitle());
    } catch (RepositoryException re) {
      if(LOG.isErrorEnabled())
        LOG.error("Can not get the repository. ", re);
    }
    fileActivity.setContentNode(contentNode);
  }
}
