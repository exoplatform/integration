/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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

import java.io.InputStream;
import java.io.StringWriter;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.application.PortalApplication;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.Orientation;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.plugin.doc.UIDocViewer;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.application.Application;
import org.exoplatform.webui.application.WebuiApplication;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.gatein.api.site.SiteType;

/**
 *
 */
@Path("/contentviewer")
public class ContentViewerRESTService implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(ContentViewerRESTService.class.getName());

  private RepositoryService repositoryService;

  public ContentViewerRESTService(RepositoryService repositoryService) throws Exception {
    this.repositoryService = repositoryService;
  }

  /**
   * Returns a pdf file for a PDF document.
   *
   * @param repoName The repository name.
   * @param workspaceName   The workspace name.
   * @param uuid     The identifier of the document.
   * @return Response inputstream.
   * @throws Exception The exception
   * @anchor PDFViewerRESTService.getPDFFile
   */
  @GET
  @Path("/{repoName}/{workspaceName}/{uuid}/")
    public Response getContent(@Context HttpServletRequest request,
                               @Context HttpServletResponse response,
                             @PathParam("repoName") String repoName,
                             @PathParam("workspaceName") String workspaceName,
                             @PathParam("uuid") String uuid) throws Exception {
    String content = null;
    try {
      ManageableRepository repository = repositoryService.getCurrentRepository();
      Session session = getSystemProvider().getSession(workspaceName, repository);
      Node contentNode = session.getNodeByUUID(uuid);

      StringWriter writer = new StringWriter();

      UIDocViewer uiDocViewer = new UIDocViewer();
      uiDocViewer.docPath = contentNode.getPath();
      uiDocViewer.repository = repository.getConfiguration().getName();
      uiDocViewer.workspace = workspaceName;
      uiDocViewer.setOriginalNode(contentNode);
      uiDocViewer.setNode(contentNode);

      WebAppController controller = (WebAppController) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WebAppController.class);

      ControllerContext controllerContext = new ControllerContext(controller, controller.getRouter(), request, response, null);
      PortalApplication application = controller.getApplication(PortalApplication.PORTAL_APPLICATION_ID);
      PortalRequestContext requestContext = new PortalRequestContext(application, controllerContext, org.exoplatform.portal.mop.SiteType.PORTAL.toString(), "", "", null);
      WebuiRequestContext.setCurrentInstance(requestContext);
      requestContext.setUIApplication(new UIPortalApplication());
      requestContext.setWriter(writer);

      uiDocViewer.processRender(requestContext);

      content = writer.toString();

    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(e);
      }
    }
    return Response.ok(content).build();
  }

  private SessionProvider getSystemProvider() {
    SessionProviderService service = WCMCoreUtils.getService(SessionProviderService.class);
    return service.getSystemSessionProvider(null);
  }

  public class MyPortalRequestContext extends PortalRequestContext {
    public MyPortalRequestContext(WebuiApplication app, ControllerContext controllerContext, String requestSiteType, String requestSiteName, String requestPath, Locale requestLocale) {
      super(app, controllerContext, requestSiteType, requestSiteName, requestPath, requestLocale);
    }

    @Override
    public Orientation getOrientation() {
      return Orientation.LT;
    }
  }

}
