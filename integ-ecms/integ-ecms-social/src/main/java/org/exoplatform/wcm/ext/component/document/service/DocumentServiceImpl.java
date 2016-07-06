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
package org.exoplatform.wcm.ext.component.document.service;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.wcm.ext.component.document.model.Document;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 22, 2011
 */
public class DocumentServiceImpl implements DocumentService {

  private static final String MIX_REFERENCEABLE = "mix:referenceable";
  private static final String EXO_LAST_MODIFIER_PROP = "exo:lastModifier";
  private static final String EXO_DATE_CREATED_PROP = "exo:dateCreated";
  private static final String JCR_LAST_MODIFIED_PROP = "jcr:lastModified";
  private static final String JCR_CONTENT = "jcr:content";
  private static final String EXO_OWNER_PROP = "exo:owner";
  private static final String EXO_TITLE_PROP = "exo:title";
  private static final String CURRENT_STATE_PROP = "publication:currentState";
  private static final String GROUPS_DRIVE_NAME = "Groups";
  private static final String GROUPS_DRIVE_ROOT_NODE = "Groups";
  private static final String PERSONAL_DRIVE_NAME = "Personal Documents";
  private static final String PERSONAL_DRIVE_ROOT_NODE = "Users";

  private ManageDriveService manageDriveService;

  public DocumentServiceImpl(ManageDriveService manageDriveService) {
    this.manageDriveService = manageDriveService;
  }

  @Override
  public Document findDocById(String documentId) throws RepositoryException {
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    ManageableRepository manageRepo = repositoryService.getCurrentRepository();
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();

    String ws = documentId.split(":/")[0];
    String uuid = documentId.split(":/")[1];

    Node node = sessionProvider.getSession(ws, manageRepo).getNodeByUUID(uuid);
    // Create Document
    String title = node.hasProperty(EXO_TITLE_PROP) ? node.getProperty(EXO_TITLE_PROP).getString() : "";
    String id = node.isNodeType(MIX_REFERENCEABLE) ? node.getUUID() : "";
    String state = node.hasProperty(CURRENT_STATE_PROP) ? node.getProperty(CURRENT_STATE_PROP).getValue().getString() : "";
    String author = node.hasProperty(EXO_OWNER_PROP) ? node.getProperty(EXO_OWNER_PROP).getString() : "";
    Calendar lastModified = (node.hasNode(JCR_CONTENT) ? node.getNode(JCR_CONTENT)
                                                             .getProperty(JCR_LAST_MODIFIED_PROP)
                                                             .getValue()
                                                             .getDate() : null);
    Calendar dateCreated = (node.hasProperty(EXO_DATE_CREATED_PROP) ? node.getProperty(EXO_DATE_CREATED_PROP)
                                                                          .getValue()
                                                                          .getDate()
                                                                   : null);
    String lastEditor = (node.hasProperty(EXO_LAST_MODIFIER_PROP) ? node.getProperty(EXO_LAST_MODIFIER_PROP)
                                                                        .getValue()
                                                                        .getString()
                                                                 : "");
    Document doc = new Document(id, node.getName(), title, node.getPath(), 
                                ws, state, author, lastEditor, lastModified, dateCreated);
    return doc;
  }

  /**
   * Get link to open a document in the Documents application.
   * This method will try to guess what is the best drive to use based on the node path.
   * @param path path of the nt:file node to open
   * @return Link to open the document
   * @throws Exception
   */
  @Override
  public String getLinkInDocumentsApp(String path) throws Exception {
    if(path == null) {
      return null;
    }
    String url;
    String[] splitedPath = path.split("/");
    if (splitedPath != null && splitedPath.length >= 4
            && splitedPath[1].equals(GROUPS_DRIVE_ROOT_NODE) && splitedPath[2].equals("spaces")) {
      // use the space documents application if the document is the space documents
      String spaceName = splitedPath[3];
      url = new StringBuilder(CommonsUtils.getCurrentDomain()).append("/")
              .append(PortalContainer.getCurrentPortalContainerName())
              .append("/g/:spaces:")
              .append(spaceName)
              .append("/")
              .append(spaceName)
              .append("/documents?path=")
              .append(GROUPS_DRIVE_NAME)
              .append("/:spaces:")
              .append(spaceName)
              .append(path)
              .toString();
    } else if (splitedPath != null && splitedPath.length >= 6
            && splitedPath[1].equals(PERSONAL_DRIVE_ROOT_NODE)) {
      // use the personal documents drive if the document is the personal documents
      String userId = splitedPath[5];
      url = new StringBuilder(CommonsUtils.getCurrentDomain()).append("/")
              .append(PortalContainer.getCurrentPortalContainerName())
              // TODO remove hardcoded reference to intranet site
              .append("/intranet")
              .append("/documents?path=")
              .append(PERSONAL_DRIVE_NAME)
              .append("/:")
              .append(userId)
              .append(path)
              .toString();
    } else {
      // otherwise use the default drive
      url = getLinkInDocumentsApp(path, manageDriveService.getDriveOfDefaultWorkspace());
    }

    return url;
  }

  /**
   * Get link to open a document in the Documents application with the given drive
   * @param path path of the nt:file node to open
   * @param driveName driveName to use to open the nt:file node
   * @return Link to open the document
   * @throws Exception
   */
  @Override
  public String getLinkInDocumentsApp(String path, String driveName) throws Exception {
    if(path == null) {
      return null;
    }
    String url = new StringBuilder(CommonsUtils.getCurrentDomain()).append("/")
              .append(PortalContainer.getCurrentPortalContainerName())
              // TODO remove hardcoded reference to intranet site
              .append("/intranet")
              .append("/documents?path=")
              .append(driveName)
              .append(path)
              .toString();
    return url;
  }
}
