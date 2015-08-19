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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.ext.component.activity;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.social.webui.activity.BaseUIActivityBuilder;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 15, 2011
 */
public class FileUIActivityBuilder extends BaseUIActivityBuilder {
  private static final Log LOG = ExoLogger.getLogger(FileUIActivityBuilder.class);
  
  @Override
  protected void extendUIActivity(BaseUIActivity uiActivity, ExoSocialActivity activity) {
  	FileUIActivity fileActivity = (FileUIActivity) uiActivity;

    //set data into the UI component of activity
    if (activity.getTemplateParams() != null) {
    	fileActivity.setUIActivityData(activity.getTemplateParams());
    }
    
    //get node data
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    ManageableRepository manageRepo = null;
    Node contentNode = null;
    try {
      manageRepo = repositoryService.getCurrentRepository();
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      for (String ws : manageRepo.getWorkspaceNames()) {
        try {
          if(StringUtils.isEmpty(fileActivity.getNodeUUID())) {
            String contentLink = fileActivity.getContentLink();
            String _ws = contentLink.split("/")[0];
            String _repo = contentLink.split("/")[1];
            String nodePath = contentLink.replace(_ws + "/" + _repo, "");
            contentNode = (Node)sessionProvider.getSession(ws, manageRepo).getItem(nodePath);
            fileActivity.setContentNode(contentNode);
            return;
          }
          contentNode = sessionProvider.getSession(ws, manageRepo).getNodeByUUID(fileActivity.getNodeUUID());
          fileActivity.docPath = contentNode.getPath();
          fileActivity.workspace = ws;
          fileActivity.repository = manageRepo.toString();
          break;
        } catch (RepositoryException e) {
          continue;
        }
      }
    } catch (RepositoryException re) {
      LOG.error("Can not get the repository. ", re);
    }
      
    fileActivity.setContentNode(contentNode);
  }

}
