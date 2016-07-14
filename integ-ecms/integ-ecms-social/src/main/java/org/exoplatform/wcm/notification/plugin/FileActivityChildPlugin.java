/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
package org.exoplatform.wcm.notification.plugin;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.AbstractNotificationChildPlugin;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.NotificationUtils;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;
import org.exoplatform.webui.cssfile.CssClassIconFile;
import org.exoplatform.webui.cssfile.CssClassManager;
import org.exoplatform.webui.cssfile.CssClassUtils;


public class FileActivityChildPlugin extends AbstractNotificationChildPlugin {
  private static final Log   LOG                          = ExoLogger.getLogger(FileActivityChildPlugin.class);
  public final static ArgumentLiteral<String> ACTIVITY_ID = new ArgumentLiteral<String>(String.class, "activityId");
  public final static String PRIVATE_FOLDER_PATH          = "/Private/";
  public final static String ACTIVITY_URL                 = "view_full_activity";
  public static final String ID                           = "files:spaces";
  public static final String MESSAGE                      = "MESSAGE";
  public static final String REPOSITORY                   = "REPOSITORY";
  public static final String WORKSPACE                    = "WORKSPACE";
  public static final String DOCLINK                      = "DOCLINK";
  public static final String NODE_UUID                    = "id";
  public static final String AUTHOR                       = "author";
  public static final String MIME_TYPE                    = "mimeType";
  public static final String DOCUMENT_TITLE               = "docTitle";
  public static final String CONTENT_NAME                 = "contentName";
  public static final String DOCUMENT_SUMMARY             = "docSummary";
  public static final String EXO_RESOURCES_URI            = "/eXoSkin/skin/images/themes/default/Icons/TypeIcons/EmailNotificationIcons/";
  public static final String DOCNAME                      = "DOCNAME";
  public static final String ICON_FILE_EXTENSION          = ".png";

  private String             mimeType;
  private String             nodeUUID;
  private Node               contentNode;
  private NodeLocation       nodeLocation;
  private String             documentTitle;
  private ExoSocialActivity  activity;
  private String             repository;
  private String             workspace;
  private String             baseURI;
  private String             docName;


  public FileActivityChildPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String makeContent(NotificationContext ctx) {
    try {
      ActivityManager activityM = CommonsUtils.getService(ActivityManager.class);

      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);
      TemplateContext templateContext = new TemplateContext(ID, language);

      String activityId = notification.getValueOwnerParameter(ACTIVITY_ID.getKey());
      activity = activityM.getActivity(activityId);

      if (activity.isComment()) {
        activity = activityM.getParentActivity(activity);  
      }

      //

      Map<String, String> templateParams = activity.getTemplateParams();
      getAndSetFileInfo(templateParams);

      //
      Node currentNode = getContentNode();
      
      // File uploaded to Content Explorer hasn't MESSAGE field
      String message = templateParams.get(MESSAGE) != null ? NotificationUtils.processLinkTitle(templateParams.get(MESSAGE)) : "";
      String nodePath = nodeLocation.getPath();
      String[] splitedPath = nodePath.split("/");
      String contentWorkspace = (workspace != null) ? workspace : templateParams.get("workspace");
      if (splitedPath[1].equals("Groups") && splitedPath[2].equals("spaces")) {
        templateContext.put("ACTIVITY_URL", CommonsUtils.getCurrentDomain() + LinkProvider.getRedirectSpaceUri(getSpaceDocuments(splitedPath[3]) +
                "?path=" + capitalizeFirstLetter(contentWorkspace) + nodePath + "&notification=true"));
      } else {
        templateContext.put("ACTIVITY_URL", CommonsUtils.getCurrentDomain() + LinkProvider.getRedirectUri("documents" +
                "?path=" + capitalizeFirstLetter(contentWorkspace) + nodePath + "&notification=true"));
      }
      templateContext.put("ACTIVITY_TITLE", message);
      templateContext.put("DOCUMENT_TITLE", this.documentTitle);
      templateContext.put("SUMMARY", Utils.getSummary(currentNode));
      templateContext.put("SIZE", getSize(currentNode));
      templateContext.put("VERSION", getVersion(currentNode));
      templateContext.put("IS_VIDEO", this.mimeType.startsWith("video"));

      String thumbnailUrl = null;
      String docLink = templateParams.get(DOCLINK);
      String author = templateParams.get(AUTHOR);
      String receiver = notification.getTo();
      if (docLink != null && docLink.contains(author + PRIVATE_FOLDER_PATH) && !(StringUtils.equals(author, receiver))) {
        templateContext.put("DEFAULT_THUMBNAIL_URL", getDefaultThumbnail());
      } else {
        thumbnailUrl = getThumbnailUrl(currentNode);
        if (thumbnailUrl == null) {
          templateContext.put("DEFAULT_THUMBNAIL_URL", getDefaultThumbnail());
        }
      }

      templateContext.put("THUMBNAIL_URL", thumbnailUrl);

      //

      //
      String content = TemplateUtils.processGroovy(templateContext);
      return content;
    } catch (Exception e) {
      LOG.error("Failed at makeContent().", e);
      return (activity != null) ? activity.getTitle() : "";
    }
  }

  private String getSpaceDocuments(String space) {
    return "g/:spaces:" + space + "/" +space + "/" + "documents";
  }

  private String capitalizeFirstLetter(String str) throws Exception {
    return str.substring(0, 1).toUpperCase() + str.substring(1);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return false;
  }
  
  private void getAndSetFileInfo(Map<String, String> templateParams) {
    this.repository = templateParams.get(REPOSITORY);
    this.workspace = templateParams.get(WORKSPACE);
    this.nodeUUID = templateParams.get(NODE_UUID);
    this.mimeType = templateParams.get(MIME_TYPE);
    this.docName = templateParams.get(DOCNAME);
    
    String documentTitle = templateParams.get(DOCUMENT_TITLE);
    if (documentTitle != null) {
      this.documentTitle = documentTitle;
    } else {
      this.documentTitle = templateParams.get(CONTENT_NAME);;
    }
    
    //get node data
    RepositoryService repositoryService = WCMCoreUtils.getService(RepositoryService.class);
    ManageableRepository manageRepo = null;
    try {
      manageRepo = repositoryService.getCurrentRepository();
      SessionProvider sessionProvider = CommonsUtils.getSystemSessionProvider();
      for (String ws : manageRepo.getWorkspaceNames()) {
        try {
          this.contentNode = sessionProvider.getSession(ws, manageRepo).getNodeByUUID(this.nodeUUID);
          break;
        } catch (RepositoryException e) {
          continue;
        }
      }
    } catch (RepositoryException re) {
      LOG.error("Can not get the repository. ", re);
    }

    this.nodeLocation = NodeLocation.getNodeLocationByNode(contentNode);
    
    //
    this.baseURI = CommonsUtils.getCurrentDomain();
  }

  private Object getDefaultThumbnail() {
    String cssClass = CssClassUtils.getCSSClassByFileNameAndFileType(
        this.docName, this.mimeType, CssClassManager.ICON_SIZE.ICON_64);
    
    if (cssClass.indexOf(CssClassIconFile.DEFAULT_CSS) > 0) {
      return baseURI + EXO_RESOURCES_URI  + "uiIcon64x64Templatent_file.png";
    }
    return baseURI + EXO_RESOURCES_URI + cssClass.split(" ")[0] + ICON_FILE_EXTENSION;
  }

  private String getThumbnailUrl(Node currentNode) {
    try {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
      String portalName = containerInfo.getContainerName();
      
      String restContextName = org.exoplatform.ecm.webui.utils.Utils.getRestContextName(portalName);
      String preferenceWS = currentNode.getSession().getWorkspace().getName();
      String encodedPath = URLEncoder.encode(currentNode.getPath(), "utf-8");
      encodedPath = encodedPath.replaceAll ("%2F", "/");
      
      if (this.mimeType.startsWith("image")) {
        int imageWidth = getImageWidth(currentNode);
        int imageHeight = getImageHeight(currentNode);
        if (imageHeight > imageWidth && imageHeight > 300) {
          imageWidth = (300 * imageWidth) / imageHeight;
          imageHeight = 300;
        } else if(imageWidth > imageHeight && imageWidth > 300){
          imageHeight = (300 * imageHeight) / imageWidth;
          imageWidth = 300;
        } else if(imageWidth == imageHeight && imageHeight > 300) {
          imageWidth = 300;
          imageHeight= 300;
        }
        
        return this.baseURI + "/" + portalName + "/" + restContextName + "/thumbnailImage/custom/" + imageWidth + "x" + imageHeight + "/" +
          this.repository + "/" + preferenceWS + encodedPath;
      }
      else if (this.mimeType.indexOf("icon") >=0) {
        return getWebdavURL();
      }
      else if (org.exoplatform.services.cms.impl.Utils.isSupportThumbnailView(mimeType)) {
        return this.baseURI + "/" + portalName + "/" + restContextName + "/thumbnailImage/big/" + this.repository + "/" + preferenceWS + encodedPath;
      } else {
        return null;
      }
      
    }
    catch (Exception e) {
      LOG.debug("Cannot get thumbnail url");
    }
    return StringUtils.EMPTY;
  }

  private Node getContentNode() {
    return NodeLocation.getNodeByLocation(nodeLocation);
  }

  private String getSize(Node node) {
    double size = getFileSize(node);   
    try {
      if (node.hasNode(org.exoplatform.ecm.webui.utils.Utils.JCR_CONTENT)) {
        return FileUtils.byteCountToDisplaySize((long)size);
      }
    } catch (Exception e) {
      return StringUtils.EMPTY;
    }
    return StringUtils.EMPTY;    
  }

  private double getFileSize(Node node) {
    double fileSize = 0;    
    try {
      if (node.hasNode(org.exoplatform.ecm.webui.utils.Utils.JCR_CONTENT)) {
        Node contentNode = node.getNode(org.exoplatform.ecm.webui.utils.Utils.JCR_CONTENT);
        if (contentNode.hasProperty(org.exoplatform.ecm.webui.utils.Utils.JCR_DATA)) {
          fileSize = contentNode.getProperty(org.exoplatform.ecm.webui.utils.Utils.JCR_DATA).getLength();
        }
      }
    } catch(Exception ex) { fileSize = 0; }
    return fileSize;    
  }

  private String getWebdavURL() throws Exception {
    contentNode = getContentNode();
    FriendlyService friendlyService = WCMCoreUtils.getService(FriendlyService.class);
    String link = "#";

    String portalName = PortalContainer.getCurrentPortalContainerName();
    String restContextName = PortalContainer.getCurrentRestContextName();
    if (this.contentNode.isNodeType("nt:frozenNode")) {
      String uuid = this.contentNode.getProperty("jcr:frozenUuid").getString();
      Node originalNode = this.contentNode.getSession().getNodeByUUID(uuid);
      link = baseURI + "/" + portalName + "/" + restContextName + "/jcr/" + this.repository + "/"
          + this.workspace + originalNode.getPath() + "?version=" + this.contentNode.getParent().getName();
    } else {
      link = baseURI + "/" + portalName + "/" + restContextName + "/jcr/" + this.repository + "/"
          + this.workspace + this.contentNode.getPath();
    }

    return friendlyService.getFriendlyUri(link);
  }

  private int getImageWidth(Node node) {
    int imageWidth = 0;
    try {
      if(node.hasNode(NodetypeConstant.JCR_CONTENT)) node = node.getNode(NodetypeConstant.JCR_CONTENT);
      ImageReader reader = ImageIO.getImageReadersByMIMEType(mimeType).next();
      ImageInputStream iis = ImageIO.createImageInputStream(node.getProperty("jcr:data").getStream());
      reader.setInput(iis, true);
      imageWidth = reader.getWidth(0);
      iis.close();
      reader.dispose();     
    } catch (RepositoryException | IOException e ){
      if (LOG.isWarnEnabled()) {
        LOG.warn("Can not get image width in " + this.getClass().getName());
      }
    }
    return imageWidth;
  }

  private int getImageHeight(Node node) {
    int imageHeight = 0;
    try {
      if(node.hasNode(NodetypeConstant.JCR_CONTENT)) node = node.getNode(NodetypeConstant.JCR_CONTENT);
      ImageReader reader = ImageIO.getImageReadersByMIMEType(mimeType).next();
      ImageInputStream iis = ImageIO.createImageInputStream(node.getProperty("jcr:data").getStream());
      reader.setInput(iis, true);
      imageHeight = reader.getHeight(0);
      iis.close();
      reader.dispose();     
    } catch (RepositoryException | IOException e ){
      if (LOG.isWarnEnabled()) {
        LOG.warn("Can not get image height in " + this.getClass().getName());
      }
    }
    return imageHeight;
  }

  private int getVersion(Node node) {
    String currentVersion = null;
    try {
      currentVersion = contentNode.getBaseVersion().getName();      
      if (currentVersion.contains("jcr:rootVersion")) currentVersion = "0";
    }catch (Exception e) {
      currentVersion ="0";
    }
    return Integer.parseInt(currentVersion);
  }
}
