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
package org.exoplatform.social.ckeditor.listener;

import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.util.UriEncoder;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.ecm.connector.platform.ManageDocumentService;
import org.exoplatform.ecm.utils.text.Text;
import org.exoplatform.services.cms.drives.DriveData;
import org.exoplatform.services.cms.drives.ManageDriveService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.activity.ActivityLifeCycleEvent;
import org.exoplatform.social.core.activity.ActivityListenerPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wcm.connector.FileUploadHandler;

/**
 * This class is used as a listener that detects uploaded images on activity/comment/reply
 * and store them on Personal Documents or Space Documen (if the activity is of type space)
 * Once the file is stored, the activity message will be modified to use the WebDAV URL of
 * image.
 * 
 */
public class ActivityImageLinkUpdateListener extends ActivityListenerPlugin {
  public static final String       IP_REGEX                            =
                                            "(((((25[0-5])|(2[0-4][0-9])|([01]?[0-9]?[0-9]))\\.){3}((25[0-4])|(2[0-4][0-9])|((1?[1-9]?[1-9])|([1-9]0))))|(0\\.){3}0)";

  public static final String       URL_OR_URI_REGEX                    = "^(((ht|f)tp(s?)://)"                                                                                                                                                                                                                                                                                                                              // protocol
      + "(\\w+(:\\w+)?@)?"                                                                                                                                                                                                                                                                                                                                                                                                 // username:password@
      + "(" + IP_REGEX                                                                                                                                                                                                                                                                                                                                                                                                     // ip
      + "|([0-9a-z_!~*'()-]+\\.)*([0-9a-z][0-9a-z-]{0,61})?[0-9a-z]\\.[a-z]{2,6}"                                                                                                                                                                                                                                                                                                                                          // domain
                                                                                                                                                                                                                                                                                                                                                                                                                           // like
                                                                                                                                                                                                                                                                                                                                                                                                                           // www.exoplatform.org
      + "|([a-zA-Z][-a-zA-Z0-9]+))"                                                                                                                                                                                                                                                                                                                                                                                        // domain
                                                                                                                                                                                                                                                                                                                                                                                                                           // like
                                                                                                                                                                                                                                                                                                                                                                                                                           // localhost
      + "(:[0-9]{1,5})?)?"                                                                                                                                                                                                                                                                                                                                                                                                   // port
                                                                                                                                                                                                                                                                                                                                                                                                                           // number
                                                                                                                                                                                                                                                                                                                                                                                                                           // :8080
      + "((/?)|(/[0-9a-zA-Z_!~*'().;?:@&=+$,%#-]+)+/?)$";                                                                                                                                                                                                                                                                                                                                                                  // uri

  private static final String      PERSONAL_DOCUMENTS_DRIVE_NAME_PARAM = "personal.drive.name";

  private static final String      PERSONAL_DOCUMENTS_DRIVE_NAME       = "Personal Documents";

  private static final String      SPACE_DOCUMENTS_FOLDER              = "Activity Stream Documents/Pictures";

  private static final String      PERSONAL_DOCUMENTS_FOLDER           = "Public/Activity Stream Documents/Pictures";

  private static final String      UPLOAD_ID_PARAMETER                 = "uploadId=";

  private static final Pattern     UPLOAD_ID_PATTERN                   = Pattern.compile("uploadId=(([0-9]|[a-f]|[A-F])*)");

  private static final Pattern     UPLOAD_URL_PATTERN                  = Pattern.compile(URL_OR_URI_REGEX);

  private static final Log         LOG                                 =
                                       ExoLogger.getLogger(ActivityImageLinkUpdateListener.class);

  private final PortalContainer    portalContainer;

  private final ActivityManager    activityManager;

  private final IdentityManager    identityManager;

  private final ManageDriveService driveService;

  private final UploadService      uploadService;

  private final LinkManager        linkManager;

  private final RepositoryService  repositoryService;

  private final SpaceService spaceService;

  // This couldn't be injected by constructor because it makes
  // the container fail on startup
  private ManageDocumentService    documentService;

  private String                   personalDriveName                   = PERSONAL_DOCUMENTS_DRIVE_NAME;

  private String                   repositoryName;

  public ActivityImageLinkUpdateListener(PortalContainer portalContainer,
                                         RepositoryService repositoryService,
                                         UploadService uploadService,
                                         LinkManager linkManager,
                                         ActivityManager activityManager,
                                         IdentityManager identityManager,
                                         ManageDriveService driveService,
                                         SpaceService spaceService,
                                         InitParams params) {
    this.activityManager = activityManager;
    this.identityManager = identityManager;
    this.driveService = driveService;
    this.uploadService = uploadService;
    this.linkManager = linkManager;
    this.repositoryService = repositoryService;
    this.portalContainer = portalContainer;
    this.spaceService = spaceService;
    if (params != null) {
      ValueParam personalDocumentsDriveName = params.getValueParam(PERSONAL_DOCUMENTS_DRIVE_NAME_PARAM);
      if (personalDocumentsDriveName != null) {
        personalDriveName = personalDocumentsDriveName.getValue();
      }
    }
  }

  @Override
  public void saveActivity(ActivityLifeCycleEvent event) {
    try {
      updateImageLink(event);
    } catch (Exception e) {
      LOG.warn("Error while processing activity body for attached images", e);
    }
  }

  @Override
  public void updateActivity(ActivityLifeCycleEvent event) {
    try {
      updateImageLink(event);
    } catch (Exception e) {
      LOG.warn("Error while processing activity body for attached images", e);
    }
  }

  @Override
  public void saveComment(ActivityLifeCycleEvent event) {
    try {
      updateImageLink(event);
    } catch (Exception e) {
      LOG.warn("Error while processing activity body for attached images", e);
    }
  }

  @Override
  public void likeActivity(ActivityLifeCycleEvent event) {
  }

  @Override
  public void likeComment(ActivityLifeCycleEvent event) {
  }

  private void updateImageLink(ActivityLifeCycleEvent event) throws Exception {
    ExoSocialActivity activity = event.getActivity();
    String body = activity.getBody();
    String title = activity.getTitle();
    boolean storeActivity = false;

    if (StringUtils.isNotBlank(body) && body.contains(UPLOAD_ID_PARAMETER)) {
      body = getModifiedLink(activity, body);
      if (!StringUtils.equals(body, activity.getBody())) {
        activity.setBody(body);
        storeActivity = true;
      }
    }

    if (StringUtils.isNotBlank(title) && title.contains(UPLOAD_ID_PARAMETER)) {
      title = getModifiedLink(activity, title);
      if (!StringUtils.equals(title, activity.getTitle())) {
        activity.setTitle(title);
        storeActivity = true;
      }
    }

    if (storeActivity) {
      activityManager.updateActivity(activity);
    }
  }

  private String getModifiedLink(ExoSocialActivity activity, String body) throws Exception, RepositoryException {
    Set<String> processedUploads = new HashSet<>();
    Matcher matcher = UPLOAD_ID_PATTERN.matcher(body);
    if (!matcher.find()) {
      return body;
    }

    String posterId = activity.getPosterId();
    String userName = identityManager.getIdentity(posterId, false).getRemoteId();

    YearMonth yearMonth = YearMonth.now();
    int year = yearMonth.getYear();
    int month = yearMonth.getMonthValue();
    String monthString = String.format("%02d", month);

    String currentFolder = null;
    DriveData selectedDriveData = null;

    if (activity.getActivityStream().getType() != null && SpaceIdentityProvider.NAME.equals(activity.getActivityStream().getType().toString())) {
      currentFolder = SPACE_DOCUMENTS_FOLDER + "/" + year + "/" + monthString;
      String streamOwner = activity.getStreamOwner();
      Space space = spaceService.getSpaceByPrettyName(streamOwner);
      if (space == null) {
        LOG.warn("Can't find space with pretty name: {}. The uploaded files on activity {} will be ignored.", streamOwner, activity.getId());
      }
      selectedDriveData = driveService.getDriveByName(space.getGroupId().replaceAll("/", "."));
    } else {
      currentFolder = PERSONAL_DOCUMENTS_FOLDER + "/" + year + "/" + monthString;
      List<DriveData> personalDrives = driveService.getPersonalDrives(userName);
      if (personalDrives == null || personalDrives.isEmpty()) {
        LOG.warn("The user {} hasn't personal drives, thus the uploaded files will be deleted from teporary folder", userName);
        return body;
      }
      for (DriveData driveData : personalDrives) {
        if (personalDriveName.equals(driveData.getName())) {
          selectedDriveData = driveData;
          break;
        }
      }
      if (selectedDriveData == null) {
        selectedDriveData = personalDrives.get(0);
        LOG.warn("Cannot find configured personal drive with name {}, another drive will be used instead: {}",
                 personalDriveName,
                 selectedDriveData.getName());
      }
    }

    String originalBody = body;
    // matcher.find has already been called, thus,
    // no need to call it another time, do..while loop is used
    do {
      String uploadId = matcher.group(matcher.groupCount() - 1);
      if (!processedUploads.contains(uploadId)) {

        UploadResource uploadedResource = uploadService.getUploadResource(uploadId);
        String fileName = uploadedResource.getFileName();

        Node parentForlderNode = getNode(selectedDriveData, currentFolder, userName);
        int i = 1;
        String originalfileName = fileName;
        while (parentForlderNode.hasNode(fileName)) {
          if (originalfileName.contains(".")) {
            int indexOfPoint = originalfileName.indexOf(".");
            fileName = originalfileName.substring(0, indexOfPoint) + "(" + i + ")" + originalfileName.substring(indexOfPoint);
          } else {
            fileName = originalfileName + "(" + i + ")";
          }
          i++;
        }

        fileName = Text.escapeIllegalJcrChars(fileName);
        fileName = Utils.cleanName(fileName);

        Response uploadResponse = getDocumentService().processUpload(selectedDriveData.getWorkspace(),
                                                                     selectedDriveData.getName(),
                                                                     currentFolder,
                                                                     null,
                                                                     FileUploadHandler.SAVE_ACTION,
                                                                     null,
                                                                     fileName,
                                                                     uploadId,
                                                                     FileUploadHandler.KEEP_BOTH);
        if (uploadResponse.getStatus() != 200) {
          LOG.warn("Error while uploading file with upload id: {}, name: {}, cause = {}",
                   uploadId,
                   fileName,
                   uploadResponse.getEntity());
          continue;
        }
        // Refresh parent folder
        parentForlderNode = getNode(selectedDriveData, currentFolder, userName);
        fileName = Utils.cleanNameWithAccents(fileName);
        fileName = Utils.cleanName(fileName);
        if (!parentForlderNode.hasNode(fileName)) {
          LOG.warn("Cannot find attached file in JCR with upload id: {}, name: {}", uploadId, fileName);
          continue;
        }

        int uploadIdIndex = matcher.start();
        String urlToReplace = getURLToReplace(originalBody, uploadId, uploadIdIndex);
        if (!UPLOAD_URL_PATTERN.matcher(urlToReplace).matches()) {
          LOG.warn("Unrecognized URL to replace in activity body {}", urlToReplace);
          continue;
        }

        String fileURI = CommonsUtils.getCurrentDomain() + getJcrURI(parentForlderNode, fileName);
        if (StringUtils.isNotBlank(urlToReplace)) {
          // don't use replaceAll because this method is using regex
          // we will iterate on all occurrences until it's replace
          // in all body
          while (body.contains(urlToReplace)) {
            body = body.replace(urlToReplace, UriEncoder.encode(fileURI));
          }
          processedUploads.add(uploadId);
        }
      }
    } while (matcher.find());
    return body;
  }

  private String getJcrURI(Node parentForlderNode, String fileName) throws RepositoryException {
    Node fileNode = parentForlderNode.getNode(fileName);
    return "/" + portalContainer.getName() + "/" + portalContainer.getRestContextName() + "/jcr/" + getRepositoryName() + "/"
        + fileNode.getSession().getWorkspace().getName() + fileNode.getPath();
  }

  private static String getURLToReplace(String body, String uploadId, int uploadIdIndex) {
    int srcBeginIndex = body.lastIndexOf("\"", uploadIdIndex);
    int srcEndIndex = -1;
    if (srcBeginIndex < 0) {
      srcBeginIndex = body.lastIndexOf("'", uploadIdIndex);
      if (srcBeginIndex < 0) {
        LOG.warn("Cannot find src start delimiter in URL for uploadId " + uploadId + " ignore URL replacing");
      } else {
        srcEndIndex = body.indexOf("'", srcBeginIndex + 1);
      }
    } else {
      srcEndIndex = body.indexOf("\"", srcBeginIndex + 1);
    }
    String urlToReplace = null;
    if (srcEndIndex < 0) {
      LOG.warn("Cannot find src end delimiter in URL for uploadId " + uploadId + " ignore URL replacing");
    } else {
      urlToReplace = body.substring(srcBeginIndex + 1, srcEndIndex);
    }
    return urlToReplace;
  }

  private Node getNode(DriveData driveData, String currentFolder, String userId) throws Exception {
    Session session = getSession(driveData.getWorkspace());
    String driveHomePath = driveData.getHomePath();
    String drivePath = driveHomePath;
    if (driveData.getName().equals(personalDriveName)) {
      drivePath = Utils.getPersonalDrivePath(driveHomePath, userId);
    }
    Node node = (Node) session.getItem(Text.escapeIllegalJcrChars(drivePath));
    if (StringUtils.isEmpty(currentFolder)) {
      return node;
    }
    for (String folder : currentFolder.split("/")) {
      if (StringUtils.isBlank(folder)) {
        continue;
      }
      if (node.hasNode(folder)) {
        node = node.getNode(folder);
        if (node.isNodeType(NodetypeConstant.EXO_SYMLINK))
          node = linkManager.getTarget(node);
      } else if (node.isNodeType(NodetypeConstant.EXO_SYMLINK)) {
        node = linkManager.getTarget(node).getNode(folder);
      } else {
        node = node.addNode(folder);
      }
    }
    session.save();
    return node;
  }

  private Session getSession(String workspaceName) throws Exception {
    SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
    ManageableRepository manageableRepository = repositoryService.getCurrentRepository();
    return sessionProvider.getSession(workspaceName, manageableRepository);
  }

  public ManageDocumentService getDocumentService() {
    if (documentService == null) {
      documentService = portalContainer.getComponentInstanceOfType(ManageDocumentService.class);
    }
    return documentService;
  }

  public String getRepositoryName() {
    if (repositoryName == null) {
      try {
        this.repositoryName = repositoryService.getCurrentRepository().getConfiguration().getName();
      } catch (RepositoryException e) {
        this.repositoryName = repositoryService.getConfig().getDefaultRepositoryName();
      }
    }
    return repositoryName;
  }
}
