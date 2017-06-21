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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;
import javax.portlet.PortletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import com.ibm.icu.util.Calendar;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.PortalContainerInfo;
import org.exoplatform.download.DownloadService;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.documents.TrashService;
import org.exoplatform.services.cms.documents.VersionHistoryUtils;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.impl.ManageDriveServiceImpl;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
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
import org.exoplatform.web.CacheUserProfileFilter;
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

  public static final String SEPARATOR_REGEX      = "\\|@\\|";

  private static final String NEW_DATE_FORMAT     = "hh:mm:ss MMM d, yyyy";
  
  private static final Log    LOG                 = ExoLogger.getLogger(FileUIActivity.class);

  public static final String  ID                  = "id";

  public static final String  CONTENT_LINK        = "contenLink";

  public static final String  MESSAGE             = "message";

  public static final String  ACTIVITY_STATUS     = "MESSAGE";

  public static final String  CONTENT_NAME        = "contentName";

  public static final String  IMAGE_PATH          = "imagePath";

  public static final String  MIME_TYPE           = "mimeType";

  public static final String  STATE               = "state";

  public static final String  AUTHOR              = "author";

  public static final String  DATE_CREATED        = "dateCreated";

  public static final String  LAST_MODIFIED       = "lastModified";

  public static final String  DOCUMENT_TYPE_LABEL = "docTypeLabel";

  public static final String  DOCUMENT_TITLE      = "docTitle";

  public static final String  DOCUMENT_VERSION    = "docVersion";

  public static final String  DOCUMENT_SUMMARY    = "docSummary";

  public static final String  IS_SYSTEM_COMMENT   = "isSystemComment";

  public static final String  SYSTEM_COMMENT      = "systemComment";

  private String              message;

  private Map<String, String> folderPathWithLinks;

  private String              activityStatus;

  public int                  filesCount          = 0;

  private String              activityTitle;

  private DateTimeFormatter   dateTimeFormatter;

  private DocumentService     documentService;

  private TrashService         trashService;

  List<ActivityFileAttachment> activityFileAttachments = new ArrayList<>();

  public FileUIActivity() throws Exception {
    super();
    documentService = CommonsUtils.getService(DocumentService.class);
    trashService = CommonsUtils.getService(TrashService.class);
    addChild(UIPopupContainer.class, null, "UIDocViewerPopupContainer");
  }

  public String getActivityTitle() {
    return activityTitle;
  }

  public void setActivityTitle(String activityTitle) {
    this.activityTitle = activityTitle;
  }

  public String getContentLink(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    return activityFileAttachments.get(i).getContentLink();
  }

  public void setContentLink(int i,String contentLink) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setContentLink(contentLink);
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getContentName(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getContentName();
  }

  public void setContentName(String contentName, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setContentName(contentName);
  }

  public String getImagePath(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getImagePath();
  }

  public void setImagePath(String imagePath, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setImagePath(imagePath);
  }

  public String getMimeType(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getMimeType();
  }

  public void setMimeType(String mimeType, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setMimeType(mimeType);
  }

  public String getNodeUUID(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getNodeUUID();
  }

  public void setNodeUUID(String nodeUUID, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setNodeUUID(nodeUUID);
  }

  public String getState(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getState();
  }

  public void setState(String state, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setState(state);
  }

  public String getAuthor(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getAuthor();
  }

  public void setAuthor(String author, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setAuthor(author);
  }

  public String getDocTypeName(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getDocTypeName();
  }

  public String getDocTitle(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getDocTitle();
  }

  public String getDocVersion(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getDocVersion();
  }

  public String getDocSummary(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getDocSummary();
  }

  public boolean isSymlink(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return false;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.isSymlink();
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

  private String convertDateUsingFormat(Calendar date, String format) throws ParseException {
    Locale locale = Util.getPortalRequestContext().getLocale();
    DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
    return dateFormat.format(date.getTime());
  }

  public String getDateCreated(int i) throws ParseException {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return convertDateFormat(activityFileAttachment.getDateCreated(), ISO8601.SIMPLE_DATETIME_FORMAT, NEW_DATE_FORMAT);
  }  

  public void setDateCreated(String dateCreated, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setDateCreated(dateCreated);
  }

  public String getLastModified(int i) throws ParseException {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return convertDateFormat(activityFileAttachment.getLastModified(), ISO8601.SIMPLE_DATETIME_FORMAT, NEW_DATE_FORMAT);
  }

  public void setLastModified(String lastModified, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setLastModified(lastModified);
  }

  public Node getContentNode(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    Node tmpContentNode = activityFileAttachment.getContentNode();
    try {
      if (activityFileAttachment.getNodeLocation() != null && (tmpContentNode == null || !tmpContentNode.getSession().isLive())) {
        tmpContentNode = NodeLocation.getNodeByLocation(activityFileAttachment.getNodeLocation());
      }
    } catch (RepositoryException e) {
      if (activityFileAttachment.getNodeLocation() != null) {
        tmpContentNode = NodeLocation.getNodeByLocation(activityFileAttachment.getNodeLocation());
      }
    }
    activityFileAttachment.setContentNode(tmpContentNode);
    return tmpContentNode;
  }

  public void setContentNode(Node contentNode, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setContentNode(contentNode);
    activityFileAttachment.setNodeLocation(NodeLocation.getNodeLocationByNode(contentNode));
  }

  public NodeLocation getNodeLocation(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    return activityFileAttachment.getNodeLocation();
  }

  public void setNodeLocation(NodeLocation nodeLocation, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    activityFileAttachment.setNodeLocation(nodeLocation);
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
    if(StringUtils.isEmpty(userId)) {
      return "";
    }

    // if the requested user is the connected user, get the fullname from the ConversationState
    ConversationState currentUserState = ConversationState.getCurrent();
    Identity currentUserIdentity = currentUserState.getIdentity();
    if(currentUserIdentity != null) {
      String currentUser = currentUserIdentity.getUserId();
      if (currentUser != null && currentUser.equals(userId)) {
        User user = (User) currentUserState.getAttribute(CacheUserProfileFilter.USER_PROFILE);
        if(user != null) {
          return user.getDisplayName();
        }
      }
    }

    // if the requested user if not the connected user, fetch it from the organization service
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    OrganizationService organizationService = container.getComponentInstanceOfType(OrganizationService.class);

    try {
      User user = organizationService.getUserHandler().findUserByName(userId);
      if(user != null) {
        return user.getDisplayName();
      }
    } catch (Exception e) {
      LOG.error("Cannot get information of user " + userId + " : " + e.getMessage(), e);
    }

    return "";
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
  
  protected int getImageWidth(Node node, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return 0;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);

  	int imageWidth = 0;
  	try {
  		if(node.hasNode(NodetypeConstant.JCR_CONTENT)) node = node.getNode(NodetypeConstant.JCR_CONTENT);
    	ImageReader reader = ImageIO.getImageReadersByMIMEType(activityFileAttachment.getMimeType()).next();
    	ImageInputStream iis = ImageIO.createImageInputStream(node.getProperty("jcr:data").getStream());
    	reader.setInput(iis, true);
    	imageWidth = reader.getWidth(0);
    	iis.close();
    	reader.dispose();   	
    } catch (Exception e) {
        if(LOG.isTraceEnabled()) {
          String nodePath = null;
          try {
            nodePath = node.getPath();
          } catch(Exception exp) {
            // Nothing to log
          }
          LOG.trace("Cannot get image from node " + nodePath, e);
        }
    }
  	return imageWidth;
  }
  
  protected int getImageHeight(Node node, int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return 0;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);

  	int imageHeight = 0;
  	try {
  		if(node.hasNode(NodetypeConstant.JCR_CONTENT)) node = node.getNode(NodetypeConstant.JCR_CONTENT);
    	ImageReader reader = ImageIO.getImageReadersByMIMEType(activityFileAttachment.getMimeType()).next();
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

  protected String getDocUpdateDate(Node node) {
    String docUpdatedDate = "";
    try {
      if(node != null && node.hasProperty("exo:lastModifiedDate")) {
        String rawDocUpdatedDate = node.getProperty("exo:lastModifiedDate").getString();
        LocalDateTime parsedDate = LocalDateTime.parse(rawDocUpdatedDate, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        docUpdatedDate = parsedDate.format(getDateTimeFormatter());
      }
    } catch (RepositoryException e) {
      LOG.error("Cannot get document updated date : " + e.getMessage(), e);
    }
    return docUpdatedDate;
  }

  /**
   * Get a localized DateTimeFormatter
   * @return A localized DateTimeFormatter
   */
  protected DateTimeFormatter getDateTimeFormatter() {
    if(dateTimeFormatter == null) {
      dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
      Locale locale = WebuiRequestContext.getCurrentInstance().getLocale();
      if (locale != null) {
        dateTimeFormatter = dateTimeFormatter.withLocale(locale);
      }
    }
    return dateTimeFormatter;
  }

  protected String getDocAuthor(Node node) {
    String docAuthor = "";
    try {
      if(node != null && node.hasProperty("exo:owner")) {
        String docAuthorUsername = node.getProperty("exo:owner").getString();
        docAuthor = getUserFullName(docAuthorUsername);
      }
    } catch (RepositoryException e) {
      LOG.error("Cannot get document author : " + e.getMessage(), e);
    }
    return docAuthor;
  }

  protected int getVersion(Node node) {
    String currentVersion = null;
    try {
      if (node.isNodeType(VersionHistoryUtils.MIX_DISPLAY_VERSION_NAME) &&
              node.hasProperty(VersionHistoryUtils.MAX_VERSION_PROPERTY)) {
        //Get max version ID
        int max = (int) node.getProperty(VersionHistoryUtils.MAX_VERSION_PROPERTY).getLong();
        return max - 1;
      }
      currentVersion = node.getBaseVersion().getName();
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

  public int getFilesCount() {
    return filesCount;
  }

  public void setUIActivityData(Map<String, String> activityParams) {
    activityFileAttachments.clear();

    this.message =  activityParams.get(FileUIActivity.MESSAGE);
    this.activityStatus =  activityParams.get(FileUIActivity.ACTIVITY_STATUS);

    String[] nodeUUIDs = getParameterValues(activityParams, FileUIActivity.ID);
    this.filesCount = nodeUUIDs.length;

    String[] repositories = getParameterValues(activityParams,UIDocActivity.REPOSITORY);
    String[] workspaces = getParameterValues(activityParams,UIDocActivity.WORKSPACE);
    String[] contentLink = getParameterValues(activityParams,FileUIActivity.CONTENT_LINK);
    String[] state = getParameterValues(activityParams, FileUIActivity.STATE);
    String[] author = getParameterValues(activityParams, FileUIActivity.AUTHOR);
    String[] dateCreated =  getParameterValues(activityParams, FileUIActivity.DATE_CREATED);
    String[] lastModified =  getParameterValues(activityParams, FileUIActivity.LAST_MODIFIED);
    String[] contentName =  getParameterValues(activityParams, FileUIActivity.CONTENT_NAME);
    String[] mimeType =  getParameterValues(activityParams, FileUIActivity.MIME_TYPE);
    String[] imagePath =  getParameterValues(activityParams, FileUIActivity.IMAGE_PATH);
    String[] docTypeName =  getParameterValues(activityParams, FileUIActivity.DOCUMENT_TYPE_LABEL);
    String[] docTitle =  getParameterValues(activityParams, FileUIActivity.DOCUMENT_TITLE);  
    String[] docVersion =  getParameterValues(activityParams, FileUIActivity.DOCUMENT_VERSION);
    String[] docSummary =  getParameterValues(activityParams, FileUIActivity.DOCUMENT_SUMMARY);
    Boolean[] isSymlink = null;
    String[] isSymlinkParams = getParameterValues(activityParams, UIDocActivity.IS_SYMLINK);
    if(isSymlinkParams != null) {
      isSymlink = new Boolean[isSymlinkParams.length];
      for (int i = 0; i < isSymlinkParams.length; i++) {
        isSymlink[i] = Boolean.parseBoolean(isSymlinkParams[i]);
      }
    }

    for (int i = 0; i < this.filesCount; i++) {
      ActivityFileAttachment fileAttachment = new ActivityFileAttachment();
      String repositoryName = (String) getValueFromArray(i, repositories);
      String workspaceName = (String) getValueFromArray(i, workspaces);
      ManageableRepository repository = WCMCoreUtils.getRepository();

      if(StringUtils.isBlank(repositoryName)) {
        repositoryName = repository == null ? null : repository.getConfiguration().getName();
      }

      if(StringUtils.isBlank(workspaceName)) {
        workspaceName =  repository == null ? null : repository.getConfiguration().getDefaultWorkspaceName();
      }

      fileAttachment.setNodeUUID(nodeUUIDs[i])
                    .setRepository(repositoryName)
                    .setWorkspace(workspaceName)
                    .setContentLink((String) getValueFromArray(i, contentLink))
                    .setContentName(getValueFromArray(i, contentName))
                    .setState((String) getValueFromArray(i, state))
                    .setAuthor(getValueFromArray(i, author))
                    .setDateCreated(getValueFromArray(i, dateCreated))
                    .setLastModified(getValueFromArray(i, lastModified))
                    .setMimeType(getValueFromArray(i, mimeType))
                    .setImagePath(getValueFromArray(i, imagePath))
                    .setDocTypeName(getValueFromArray(i, docTypeName))
                    .setDocTitle(getValueFromArray(i, docTitle))
                    .setDocVersion(getValueFromArray(i, docVersion))
                    .setDocSummary(getValueFromArray(i, docSummary))
                    .setSymlink(getValueFromArray(i, isSymlink));

      Node contentNode = NodeLocation.getNodeByLocation(fileAttachment.getNodeLocation());
      if (contentNode != null) {
        try {
          if (!trashService.isInTrash(contentNode)) {
            activityFileAttachments.add(fileAttachment);
          }
        } catch (RepositoryException e) {
          LOG.error("Error while testing if the content is in trash", e);
        }
      }
    }
    this.filesCount = this.activityFileAttachments.size();
  }

  private <T> T getValueFromArray(int index, T... valuesArray) {
    return (valuesArray == null || index > (valuesArray.length - 1)) ? null : valuesArray[index];
  }

  private String[] getParameterValues(Map<String, String> activityParams, String paramName) {
    String[] values = null;
    String value = activityParams.get(paramName);
    if(value != null) {
      values = value.split(SEPARATOR_REGEX);
    }
    if (LOG.isDebugEnabled()) {
      if(this.filesCount != 0 && (values == null || values.length != this.filesCount)) {
          LOG.debug("Parameter '{}' hasn't same length as other activity parmameters", paramName);
      }
    }
    return values;
  }

  /**
   * Gets the webdav url.
   * 
   * @return the webdav url
   * @throws Exception the exception
   */
  public String getWebdavURL(int i) throws Exception {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    if (activityFileAttachment.getWebdavURL() != null) {
      return activityFileAttachment.getWebdavURL();
    }

    PortletRequestContext portletRequestContext = WebuiRequestContext.getCurrentInstance();
    PortletRequest portletRequest = portletRequestContext.getRequest();
    String repository = activityFileAttachment.getRepository();
    String workspace = activityFileAttachment.getWorkspace();
    String baseURI = portletRequest.getScheme() + "://" + portletRequest.getServerName() + ":"
        + String.format("%s", portletRequest.getServerPort());

    FriendlyService friendlyService = WCMCoreUtils.getService(FriendlyService.class);
    String link = "#";

    String portalName = PortalContainer.getCurrentPortalContainerName();
    String restContextName = PortalContainer.getCurrentRestContextName();
    Node tmpContentNode = this.getContentNode(i);
    if (tmpContentNode.isNodeType("nt:frozenNode")) {
      String uuid = tmpContentNode.getProperty("jcr:frozenUuid").getString();
      Node originalNode = tmpContentNode.getSession().getNodeByUUID(uuid);
      link = baseURI + "/" + portalName + "/" + restContextName + "/jcr/" + repository + "/"
          + workspace + originalNode.getPath() + "?version=" + this.getContentNode(i).getParent().getName();
    } else {
      link = baseURI + "/" + portalName + "/" + restContextName + "/jcr/" + repository + "/"
          + workspace + tmpContentNode.getPath();
    }

    activityFileAttachment.setWebdavURL(friendlyService.getFriendlyUri(link));
    return activityFileAttachment.getWebdavURL();
  }

  public String[] getSystemCommentBundle(Map<String, String> activityParams) {
    return org.exoplatform.wcm.ext.component.activity.listener.Utils.getSystemCommentBundle(activityParams);
  }

  public String[] getSystemCommentTitle(Map<String, String> activityParams) {
    return org.exoplatform.wcm.ext.component.activity.listener.Utils.getSystemCommentTitle(activityParams);
  }

  public DriveData getDocDrive(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
    if (activityFileAttachment.getDocDrive() != null) {
      return activityFileAttachment.getDocDrive();
    }

    NodeLocation nodeLocation = activityFileAttachment.getNodeLocation();
    if (nodeLocation != null) {
      try {
        activityFileAttachment.setDocDrive(documentService.getDriveOfNode(nodeLocation.getPath(), null, Utils.getMemberships()));
      } catch (Exception e) {
        LOG.error("Cannot get drive of node " + nodeLocation.getPath() + " : " + e.getMessage(), e);
      }
    }
    return activityFileAttachment.getDocDrive();
  }

  public Map<String, String> getDocFolderRelativePathWithLinks(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    if(folderPathWithLinks == null) {
      folderPathWithLinks = new LinkedHashMap<>();
      Map<String, String> reversedFolderPathWithLinks = new LinkedHashMap<>();

      DriveData drive = getDocDrive(i);
      if (drive != null) {
        try {
          String driveHomePath = drive.getResolvedHomePath();

          // if the drive is the Personal Documents drive, we must handle the special case of the Public symlink
          String drivePublicFolderHomePath = null;
          if (ManageDriveServiceImpl.PERSONAL_DRIVE_NAME.equals(drive.getName())) {
            drivePublicFolderHomePath = driveHomePath.replace("/" + ManageDriveServiceImpl.PERSONAL_DRIVE_PRIVATE_FOLDER_NAME, "/" + ManageDriveServiceImpl.PERSONAL_DRIVE_PUBLIC_FOLDER_NAME);
          }

          // calculate the relative path to the drive by browsing up the content node path
          Node parentContentNode = getContentNode(i).getParent();
          while (parentContentNode != null) {
            String parentPath = parentContentNode.getPath();
            // exit condition is check here instead of in the while condition to avoid
            // retrieving the path several times and because there is some logic to handle
            if (parentContentNode.getPath().equals("/") || parentPath.equals(driveHomePath)) {
              // we are at the root of the workspace or at the root of the drive
              break;
            } else if (drivePublicFolderHomePath != null && parentPath.equals(drivePublicFolderHomePath)) {
              // this is a special case : the root of the Public folder of the Personal Documents drive
              // in this case we add the Public folder in the path
              reversedFolderPathWithLinks.put(ManageDriveServiceImpl.PERSONAL_DRIVE_PUBLIC_FOLDER_NAME, getDocOpenUri(parentPath, i));
              break;
            }

            String folderName;
            // title is used if it exists, otherwise the name is used
            if (parentContentNode.hasProperty("exo:title")) {
              folderName = parentContentNode.getProperty("exo:title").getString();
            } else {
              folderName = parentContentNode.getName();
            }
            reversedFolderPathWithLinks.put(folderName, getDocOpenUri(parentPath, i));

            parentContentNode = parentContentNode.getParent();
          }
        } catch (AccessDeniedException e) {
          LOG.debug(e.getMessage());
        } catch (RepositoryException re) {
          ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);
          LOG.error("Cannot retrieve path of doc " + activityFileAttachment.getDocPath() + " : " + re.getMessage(), re);
        }
      }

      if(reversedFolderPathWithLinks.size() > 1) {
        List<Map.Entry<String, String>> entries = new ArrayList<>(reversedFolderPathWithLinks.entrySet());
        for(int j = entries.size()-1; j >= 0; j--) {
          Map.Entry<String, String> entry = entries.get(j);
          folderPathWithLinks.put(entry.getKey(), entry.getValue());
        }
      } else {
        folderPathWithLinks = reversedFolderPathWithLinks;
      }
    }

    return folderPathWithLinks;
  }

  public String getDocFolderRelativePath(int i) {
    StringBuilder folderRelativePath = new StringBuilder();

    for(String folderName : getDocFolderRelativePathWithLinks(i).keySet()) {
      folderRelativePath.append(folderName).append("/");
    }

    if(folderRelativePath.length() > 1) {
      // remove the last /
      folderRelativePath.deleteCharAt(folderRelativePath.length() - 1);
    }

    return folderRelativePath.toString();
  }

  public String getCurrentDocOpenUri(int i) {
    if ((i + 1) > activityFileAttachments.size()) {
      return null;
    }
    ActivityFileAttachment activityFileAttachment = activityFileAttachments.get(i);

    String uri = "";
    if(activityFileAttachment.getNodeLocation() != null) {
      uri = getDocOpenUri(activityFileAttachment.getDocPath(), i);
    }

    return uri;
  }

  public String getDocOpenUri(String nodePath, int i) {
    String uri = "";

    if(nodePath != null) {
      try {
        uri = documentService.getLinkInDocumentsApp(nodePath, getDocDrive(i));
      } catch(Exception e) {
        LOG.error("Cannot get document open URI of node " + nodePath + " : " + e.getMessage(), e);
        uri = "";
      }
    }

    return uri;
  }

  public String getEditLink(int i) {
    try {
      return org.exoplatform.wcm.webui.Utils.getEditLink(getContentNode(i), true, false);
    }catch (Exception e) {
      return "";
    }
  }
  
  public String getActivityEditLink(int i) {
  	try {
      return org.exoplatform.wcm.webui.Utils.getActivityEditLink(getContentNode(i));
    }catch (Exception e) {
      return "";
    }
  }

  protected String getCssClassIconFile(String fileName, String fileType, int i) {
    try {
      return org.exoplatform.ecm.webui.utils.Utils.getNodeTypeIcon(this.getContentNode(i), "uiBgd64x64");
    } catch (RepositoryException e) {
      return "uiBgd64x64FileDefault";
    }
  }

  protected String getContainerName() {
    //get portal name
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    PortalContainerInfo containerInfo = (PortalContainerInfo) container.getComponentInstanceOfType(PortalContainerInfo.class);
    return containerInfo.getContainerName();  
  }

  public String getDownloadAllLink() {
    try {
      if (activityFileAttachments.isEmpty()) {
        return null;
      }
      if(activityFileAttachments.size() == 1) {
        return getDownloadLink(0);
      }

      // Get binary data from node
      DownloadService dservice = WCMCoreUtils.getService(DownloadService.class);

      NodeLocation[] nodeLocations = new NodeLocation[activityFileAttachments.size()];

      for (int i = 0; i < activityFileAttachments.size(); i++) {
        nodeLocations[i] = activityFileAttachments.get(i).getNodeLocation();
      }

      // Make download stream
      ActivityFilesDownloadResource dresource = new ActivityFilesDownloadResource(nodeLocations);
      String fileName = "activity_" + getActivity().getId() + "_";
      Long postedTime = getActivity().getPostedTime();
      if(postedTime != null) {
        Calendar postedDate = Calendar.getInstance();
        fileName += convertDateUsingFormat(postedDate, ISO8601.COMPLETE_DATE_FORMAT).replaceAll("/", "-");
      }
      dresource.setDownloadName(fileName + ".zip");
      return dservice.getDownloadLink(dservice.addDownloadResource(dresource)) ;
    }catch (Exception e) {
      return "";
    }
  }

  public String getDownloadLink(int i) {
    try {
      return org.exoplatform.wcm.webui.Utils.getDownloadLink(getContentNode(i));
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
      String index = event.getRequestContext().getRequestParameter(OBJECTID);
      int i = Integer.parseInt(index);
      UIActivitiesContainer uiActivitiesContainer = fileUIActivity.getAncestorOfType(UIActivitiesContainer.class);
      PopupContainer uiPopupContainer = uiActivitiesContainer.getPopupContainer();

      UIDocumentPreview uiDocumentPreview = uiPopupContainer.createUIComponent(UIDocumentPreview.class, null,
              "UIDocumentPreview");
      uiDocumentPreview.setBaseUIActivity(fileUIActivity);
      if ((i + 1) > fileUIActivity.activityFileAttachments.size()) {
        return;
      }
      ActivityFileAttachment activityFileAttachment = fileUIActivity.activityFileAttachments.get(i);
      uiDocumentPreview.setContentInfo(activityFileAttachment.getDocPath(), activityFileAttachment.getRepository(), activityFileAttachment.getWorkspace(),
              fileUIActivity.getContentNode(i));

      uiPopupContainer.activate(uiDocumentPreview, 0, 0, true);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPopupContainer);
    }
  }
  
  public static class DownloadDocumentActionListener extends EventListener<FileUIActivity> {
    @Override
    public void execute(Event<FileUIActivity> event) throws Exception {
    	FileUIActivity uiComp = event.getSource();
    	String index = event.getRequestContext().getRequestParameter(OBJECTID);
    	int i = Integer.parseInt(index);
      String downloadLink = null;
      if (getRealNode(uiComp.getContentNode(i)).getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
        downloadLink = Utils.getDownloadRestServiceLink(uiComp.getContentNode(i));
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
      String index = event.getRequestContext().getRequestParameter(OBJECTID);
      int i = 0;
      if (!StringUtils.isBlank(index)) {
        i = Integer.parseInt(index);
      }

      Node currentNode = fileUIActivity.getContentNode(i);

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