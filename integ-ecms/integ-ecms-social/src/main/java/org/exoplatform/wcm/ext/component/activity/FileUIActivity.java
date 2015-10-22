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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.portlet.PortletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.friendly.FriendlyService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.SpaceStorageException;
import org.exoplatform.social.plugin.doc.UIDocActivity;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.social.webui.activity.UIActivitiesContainer;
import org.exoplatform.social.webui.composer.PopupContainer;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.ComponentConfigs;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.ext.UIExtension;
import org.exoplatform.webui.ext.UIExtensionManager;


/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 15, 2011
 */
@ComponentConfigs({
        @ComponentConfig(lifecycle = UIFormLifecycle.class,
                template = "classpath:groovy/ecm/social-integration/plugin/space/FileUIActivity.gtmpl", events = {
                @EventConfig(listeners = FileUIActivity.ViewDocumentActionListener.class),
                @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
                @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
                @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
                @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
                @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
                @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
                @EventConfig(listeners = FileUIActivity.OpenFileActionListener.class),
                @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class)}),
})
public class FileUIActivity extends BaseUIActivity{

  private static final String NEW_DATE_FORMAT = "hh:mm:ss MMM d, yyyy";

  private static final Log   LOG               = ExoLogger.getLogger(FileUIActivity.class);

  public static final String ID                 = "id";

  public static final String CONTENT_LINK       = "contenLink";

  public static final String MESSAGE            = "message";

  public static final String ACTIVITY_STATUS    = "MESSAGE";

  public static final String REPOSITORY         = "repository";

  public static final String WORKSPACE          = "workspace";

  public static final String CONTENT_NAME       = "contentName";

  public static final String IMAGE_PATH         = "imagePath";

  public static final String MIME_TYPE          = "mimeType";

  public static final String STATE              = "state";

  public static final String AUTHOR             = "author";

  public static final String DATE_CREATED       = "dateCreated";

  public static final String LAST_MODIFIED      = "lastModified";

  public static final String DOCUMENT_TYPE_LABEL= "docTypeLabel";
  
  public static final String DOCUMENT_TITLE     = "docTitle";
  
  public static final String DOCUMENT_VERSION   = "docVersion";
  
  public static final String DOCUMENT_SUMMARY   = "docSummary";

  public static final String IS_SYSTEM_COMMENT  = "isSystemComment";
  
  public static final String SYSTEM_COMMENT     = "systemComment";

  private String             contentLink;

  private String             message;

  private String             contentName;

  private String             imagePath;

  private String             mimeType;

  private String             nodeUUID;

  private String             state;

  private String             author;

  private String             dateCreated;

  private String             lastModified;

  private Node               contentNode;

  private NodeLocation       nodeLocation;
  
  private String             docTypeName;
  private String             docTitle;
  private String             docVersion;
  private String             docSummary;
  private String             activityStatus;
  public String              docPath;
  public String              repository;
  public String              workspace;

  private boolean            isSymlink;
  private String activityTitle;

  public FileUIActivity() throws Exception {
    super();
    addChild(UIPopupContainer.class, null, "UIDocViewerPopupContainer");
  }

  public String getActivityTitle() {
    return activityTitle;
  }

  public void setActivityTitle(String activityTitle) {
    this.activityTitle = activityTitle;
  }

  public String getContentLink() {
    return contentLink;
  }

  public void setContentLink(String contentLink) {
    this.contentLink = contentLink;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getContentName() {
    return contentName;
  }

  public void setContentName(String contentName) {
    this.contentName = contentName;
  }

  public String getImagePath() {
    return imagePath;
  }

  public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }

  public String getNodeUUID() {
    return nodeUUID;
  }

  public void setNodeUUID(String nodeUUID) {
    this.nodeUUID = nodeUUID;
  }

  public String getState() {
    return state;
  }

  public void setState(String state) {
    this.state = state;
  }

  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  public String getDocTypeName() {
    return docTypeName;
  }
  public String getDocTitle() {
    return docTitle;
  }
  public String getDocVersion() {
    return docVersion;
  }
  public String getDocSummary() {
    return docSummary;
  }

  public boolean isSymlink() {
    return isSymlink;
  }

  public String getTitle(Node node) throws Exception {
    return Utils.getTitle(node);
  } 
  
  private String convertDateFormat(String strDate, String strOldFormat, String strNewFormat) throws ParseException {
    if (strDate == null || strDate.length() <= 0) {
      return "";
    }
    Locale locale = Util.getPortalRequestContext().getLocale();
    SimpleDateFormat sdfSource = new SimpleDateFormat(strOldFormat);
    SimpleDateFormat sdfDestination = new SimpleDateFormat(strNewFormat, locale);
    Date date = sdfSource.parse(strDate);
    return sdfDestination.format(date);
  }

  public String getDateCreated() throws ParseException {
    return convertDateFormat(dateCreated, ISO8601.SIMPLE_DATETIME_FORMAT, NEW_DATE_FORMAT);
  }  

  public void setDateCreated(String dateCreated) {
    this.dateCreated = dateCreated;
  }

  public String getLastModified() throws ParseException {
    return convertDateFormat(lastModified, ISO8601.SIMPLE_DATETIME_FORMAT, NEW_DATE_FORMAT);
  }

  public void setLastModified(String lastModified) {
    this.lastModified = lastModified;
  }

  public Node getContentNode() {
    return NodeLocation.getNodeByLocation(nodeLocation);
  }

  public void setContentNode(Node contentNode) {
    this.nodeLocation = NodeLocation.getNodeLocationByNode(contentNode);
  }

  public NodeLocation getNodeLocation() {
    return nodeLocation;
  }

  public void setNodeLocation(NodeLocation nodeLocation) {
    this.nodeLocation = nodeLocation;
  }

  /**
   * Gets the summary.
   * @param node the node
   * @return the summary of Node. Return empty string if catch an exception.
   */
  public String getSummary(Node node) {
    return org.exoplatform.wcm.ext.component.activity.listener.Utils.getSummary(node);
  }
  
  public String getDocumentSummary(Map<String, String> activityParams) {
    return activityParams.get(FileUIActivity.DOCUMENT_SUMMARY);
  }
  public String getUserFullName(String userId) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    IdentityManager identityManager = (IdentityManager) container.getComponentInstanceOfType(IdentityManager.class);

    return identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId, true).getProfile().getFullName();
  }
  
  protected String getSize(Node node) {
    double size = 0;    
    try {
      if (node.hasNode(Utils.JCR_CONTENT)) {
        Node contentNode = node.getNode(Utils.JCR_CONTENT);
        if (contentNode.hasProperty(Utils.JCR_DATA)) {
          size = contentNode.getProperty(Utils.JCR_DATA).getLength();
        }
        
        return FileUtils.byteCountToDisplaySize((long)size);
      }
    } catch (PathNotFoundException e) {
      return StringUtils.EMPTY;
    } catch (ValueFormatException e) {
      return StringUtils.EMPTY;
    } catch (RepositoryException e) {
      return StringUtils.EMPTY;
    } catch(NullPointerException e) {
    	return StringUtils.EMPTY;
    }
    return StringUtils.EMPTY;    
  }
  
  protected double getFileSize(Node node) {
    double fileSize = 0;    
    try {
      if (node.hasNode(Utils.JCR_CONTENT)) {
        Node contentNode = node.getNode(Utils.JCR_CONTENT);
        if (contentNode.hasProperty(Utils.JCR_DATA)) {
        	fileSize = contentNode.getProperty(Utils.JCR_DATA).getLength();
        }
      }
    } catch(Exception ex) { fileSize = 0; }
    return fileSize;    
  }
  
  protected int getImageWidth(Node node) {
  	int imageWidth = 0;
  	try {
  		if(node.hasNode(NodetypeConstant.JCR_CONTENT)) node = node.getNode(NodetypeConstant.JCR_CONTENT);
    	ImageReader reader = ImageIO.getImageReadersByMIMEType(mimeType).next();
    	ImageInputStream iis = ImageIO.createImageInputStream(node.getProperty("jcr:data").getStream());
    	reader.setInput(iis, true);
    	imageWidth = reader.getWidth(0);
    	iis.close();
    	reader.dispose();   	
    } catch (Exception e) {
        LOG.info("Cannot get node");
    }
  	return imageWidth;
  }
  
  protected int getImageHeight(Node node) {
  	int imageHeight = 0;
  	try {
  		if(node.hasNode(NodetypeConstant.JCR_CONTENT)) node = node.getNode(NodetypeConstant.JCR_CONTENT);
    	ImageReader reader = ImageIO.getImageReadersByMIMEType(mimeType).next();
    	ImageInputStream iis = ImageIO.createImageInputStream(node.getProperty("jcr:data").getStream());
    	reader.setInput(iis, true);
    	imageHeight = reader.getHeight(0);
    	iis.close();
    	reader.dispose();   	
    } catch (Exception e) {
        LOG.info("Cannot get node");
    }
  	return imageHeight;
  }
  
  protected int getVersion(Node node) {
  	String currentVersion = null;
  	try {
      currentVersion = contentNode.getBaseVersion().getName();      
      if (currentVersion.contains("jcr:rootVersion")) currentVersion = "0";
    }catch (Exception e) {
      currentVersion ="0";
    }
  	return Integer.parseInt(currentVersion);
  }

  public String getUserProfileUri(String userId) {
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    IdentityManager identityManager = (IdentityManager) container.getComponentInstanceOfType(IdentityManager.class);

    return identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId, true).getProfile().getUrl();
  }

  public String getUserAvatarImageSource(String userId) {
    return getOwnerIdentity().getProfile().getAvatarUrl();
  }

  public String getSpaceAvatarImageSource(String spaceIdentityId) {
    try {
      String spaceId = getOwnerIdentity().getRemoteId();
      SpaceService spaceService = getApplicationComponent(SpaceService.class);
      Space space = spaceService.getSpaceById(spaceId);
      if (space != null) {
        return space.getAvatarUrl();
      }
    } catch (SpaceStorageException e) {
      LOG.warn("Failed to getSpaceById: " + spaceIdentityId, e);
    }
    return null;
  }

  public String getActivityStatus() {
    if (message == null) {
      return activityStatus;
    } else {
      return null;
    }
  }

  public void setUIActivityData(Map<String, String> activityParams) {
    this.contentLink = activityParams.get(FileUIActivity.CONTENT_LINK);
    this.nodeUUID = activityParams.get(FileUIActivity.ID);
    this.state = activityParams.get(FileUIActivity.STATE);
    this.author = activityParams.get(FileUIActivity.AUTHOR);
    this.dateCreated = activityParams.get(FileUIActivity.DATE_CREATED);
    this.lastModified = activityParams.get(FileUIActivity.LAST_MODIFIED);
    this.contentName = activityParams.get(FileUIActivity.CONTENT_NAME);
    this.message = activityParams.get(FileUIActivity.MESSAGE);
    this.mimeType = activityParams.get(FileUIActivity.MIME_TYPE);
    this.imagePath = activityParams.get(FileUIActivity.IMAGE_PATH);
    this.docTypeName = activityParams.get(FileUIActivity.DOCUMENT_TYPE_LABEL);
    this.docTitle = activityParams.get(FileUIActivity.DOCUMENT_TITLE);  
    this.docVersion = activityParams.get(FileUIActivity.DOCUMENT_VERSION);
    this.docSummary = activityParams.get(FileUIActivity.DOCUMENT_SUMMARY);
    this.activityStatus = activityParams.get(FileUIActivity.ACTIVITY_STATUS);
    this.isSymlink = Boolean.parseBoolean(activityParams.get(UIDocActivity.IS_SYMLINK));
  }



  /**
   * Gets the webdav url.
   * 
   * @return the webdav url
   * @throws Exception the exception
   */
  public String getWebdavURL() throws Exception {
    contentNode = getContentNode();
    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletRequest portletRequest = portletRequestContext.getRequest();
    String repository = nodeLocation.getRepository();
    String workspace = nodeLocation.getWorkspace();
    String baseURI = portletRequest.getScheme() + "://" + portletRequest.getServerName() + ":"
        + String.format("%s", portletRequest.getServerPort());

    FriendlyService friendlyService = WCMCoreUtils.getService(FriendlyService.class);
    String link = "#";

    String portalName = PortalContainer.getCurrentPortalContainerName();
    String restContextName = PortalContainer.getCurrentRestContextName();
    if (this.contentNode.isNodeType("nt:frozenNode")) {
      String uuid = this.contentNode.getProperty("jcr:frozenUuid").getString();
      Node originalNode = this.contentNode.getSession().getNodeByUUID(uuid);
      link = baseURI + "/" + portalName + "/" + restContextName + "/jcr/" + repository + "/"
          + workspace + originalNode.getPath() + "?version=" + this.contentNode.getParent().getName();
    } else {
      link = baseURI + "/" + portalName + "/" + restContextName + "/jcr/" + repository + "/"
          + workspace + this.contentNode.getPath();
    }

    return friendlyService.getFriendlyUri(link);
  }

  public String[] getSystemCommentBundle(Map<String, String> activityParams) {
    return org.exoplatform.wcm.ext.component.activity.listener.Utils.getSystemCommentBundle(activityParams);
  }

  public String[] getSystemCommentTitle(Map<String, String> activityParams) {
    return org.exoplatform.wcm.ext.component.activity.listener.Utils.getSystemCommentTitle(activityParams);
  }

  public String getViewLink() {
    try {
      Node data = getContentNode();
      if (isFileSupportPreview(data)) {
        return this.event("ViewDocument", this.getId(), "");
      } else {
        return org.exoplatform.wcm.webui.Utils.getEditLink(data, false, false);
      }
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return StringUtils.EMPTY;
    }
  }

  public String getEditLink() {
    try {
      return org.exoplatform.wcm.webui.Utils.getEditLink(getContentNode(), true, false);
    }catch (Exception e) {
      return "";
    }
  }
  
  public String getActivityEditLink() {
  	try {
      return org.exoplatform.wcm.webui.Utils.getActivityEditLink(getContentNode());
    }catch (Exception e) {
      return "";
    }
  }

  protected String getCssClassIconFile(String fileName, String fileType) {
    try {
      return org.exoplatform.ecm.webui.utils.Utils.getNodeTypeIcon(this.getContentNode(), "uiIcon64x64");
    } catch (RepositoryException e) {
      return "uiIcon64x64Templatent_file uiIcon64x64nt_file";
    }
  }

  protected String getContainerName() {
    //get portal name
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
    return containerInfo.getContainerName();  
  }
  
  public String getDownloadLink() {
    try {
      return org.exoplatform.wcm.webui.Utils.getDownloadLink(getContentNode());
    }catch (Exception e) {
      return "";
    }
  }

  /**
   * <h2>Check if file node is supported by preview on activity stream
   * A preview from the activity stream is available for the following contents:
   * </h2>
   * <ul>
   * <li>pdf and office file</li>
   * <li>media (audio, video, image)</li>
   * </ul>
   * @param data Content node
   * @return true: support; false: not support
   * @throws Exception
   */
  public boolean isFileSupportPreview(Node data) throws Exception {
    if (data != null && data.isNodeType(Utils.NT_FILE)) {
      UIExtensionManager manager = getApplicationComponent(UIExtensionManager.class);
      List<UIExtension> extensions = manager.getUIExtensions(Utils.FILE_VIEWER_EXTENSION_TYPE);

      Map<String, Object> context = new HashMap<String, Object>();
      context.put(Utils.MIME_TYPE, data.getNode(Utils.JCR_CONTENT).getProperty(Utils.JCR_MIMETYPE).getString());

      for (UIExtension extension : extensions) {
        if (manager.accept(Utils.FILE_VIEWER_EXTENSION_TYPE, extension.getName(), context) && !"Text".equals(extension.getName())) {
          return true;
        }
      }
    }

    return false;
  }

  public static class ViewDocumentActionListener extends EventListener<FileUIActivity> {
    @Override
    public void execute(Event<FileUIActivity> event) throws Exception {
      FileUIActivity fileUIActivity = event.getSource();
      UIActivitiesContainer uiActivitiesContainer = fileUIActivity.getAncestorOfType(UIActivitiesContainer.class);
      PopupContainer uiPopupContainer = uiActivitiesContainer.getPopupContainer();

      UIDocumentPreview uiDocumentPreview = uiPopupContainer.createUIComponent(UIDocumentPreview.class, null,
              "UIDocumentPreview");
      uiDocumentPreview.setBaseUIActivity(fileUIActivity);
      uiDocumentPreview.setContentInfo(fileUIActivity.docPath, fileUIActivity.repository, fileUIActivity.workspace,
              fileUIActivity.getContentNode());

      uiPopupContainer.activate(uiDocumentPreview, 0, 0, true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }
  
  public static class DownloadDocumentActionListener extends EventListener<FileUIActivity> {
    @Override
    public void execute(Event<FileUIActivity> event) throws Exception {
    	FileUIActivity uiComp = event.getSource() ;
      String downloadLink = null;
      if (getRealNode(uiComp.getContentNode()).getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
        downloadLink = Utils.getDownloadRestServiceLink(uiComp.getContentNode());
      }
      event.getRequestContext().getJavascriptManager().addJavascript("ajaxRedirect('" + downloadLink + "');");
    }
    
    private Node getRealNode(Node node) throws Exception {
      // TODO: Need to add to check symlink node
      if (node.isNodeType("nt:frozenNode")) {
        String uuid = node.getProperty("jcr:frozenUuid").getString();
        return node.getSession().getNodeByUUID(uuid);
      }
      return node;
    }
  }

  public static class OpenFileActionListener extends EventListener<FileUIActivity> {
    public void execute(Event<FileUIActivity> event) throws Exception {
      FileUIActivity fileUIActivity = event.getSource();

      Node currentNode = fileUIActivity.getContentNode();


      FileUIActivity docActivity = event.getSource();
      UIActivitiesContainer activitiesContainer = docActivity.getAncestorOfType(UIActivitiesContainer.class);
      PopupContainer popupContainer = activitiesContainer.getPopupContainer();

      org.exoplatform.ecm.webui.utils.Utils.openDocumentInDesktop(currentNode, popupContainer, event);
    }
  }

  /**
   * <h2>Check file node can edit on activity stream</h2>
   * The file only can edit when user have modify permission on parent folder
   * @param data File node
   * @return true: can edit; false: cannot edit
   * @throws Exception
   */
  public boolean canEditDocument(Node data){
    try {
      ((ExtendedNode)data.getParent()).checkPermission(PermissionType.ADD_NODE);
      return true;
    } catch(Exception e) {
      return false;
    }
  }

}