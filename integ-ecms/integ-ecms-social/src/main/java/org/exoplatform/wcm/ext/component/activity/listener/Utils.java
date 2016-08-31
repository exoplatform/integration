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
package org.exoplatform.wcm.ext.component.activity.listener;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.templates.TemplateService;
import org.exoplatform.services.context.DocumentContext;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.core.WebSchemaConfigService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.services.wcm.webcontent.WebContentSchemaHandler;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.wcm.ext.component.activity.ContentUIActivity;
import org.exoplatform.wcm.ext.component.activity.FileUIActivity;
import org.exoplatform.webui.application.WebuiRequestContext;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;



/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 18, 2011
 */
public class Utils {

  private static final Log    LOG                 = ExoLogger.getLogger(Utils.class);

  /** The Constant Activity Type */
  public static final String CONTENT_SPACES        = "contents:spaces";
  public static final String FILE_SPACES           = "files:spaces";
  public  static final String SHARE_FILE           = "sharefiles:spaces";
  public  static final String SHARE_CONTENT        = "sharecontents:spaces";

  /** the publication:currentState property name */
  private static final String CURRENT_STATE_PROP  = "publication:currentState";
  
  private static String MIX_COMMENT                = "exo:activityComment";
  private static String MIX_COMMENT_ID             = "exo:activityCommentID";
  private static int    MAX_SUMMARY_LINES_COUNT    = 4;
  private static int    MAX_SUMMARY_CHAR_COUNT     = 430;
  private static String activityType;


  public static String getActivityType() {
    return activityType;
  }

  public static void setActivityType(String activityType) {
    Utils.activityType = activityType;
  }

  /**
   * Populate activity data with the data from Node
   * 
   * @param node the node
   * @param activityOwnerId the owner id of the activity
   * @param activityMsgBundleKey the message bundle key of the activity
   * @return Map the mapped data
   */
  public static Map<String, String> populateActivityData(Node node,
          String activityOwnerId,
          String activityMsgBundleKey) throws Exception {
	  return populateActivityData(node, activityOwnerId, activityMsgBundleKey, false, null);
  }
  public static Map<String, String> populateActivityData(Node node,
                                                         String activityOwnerId, String activityMsgBundleKey, 
                                                         boolean isSystemComment, String systemComment) throws Exception {
    /** The date formatter. */
    DateFormat dateFormatter = null;
    dateFormatter = new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);
    LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);

    if(node.canAddMixin(NodetypeConstant.MIX_REFERENCEABLE)){
      node.addMixin(NodetypeConstant.MIX_REFERENCEABLE);
      node.save();
    }
    // get activity data
    String repository = ((ManageableRepository) node.getSession().getRepository()).getConfiguration()
                                                                                  .getName();
    String workspace = node.getSession().getWorkspace().getName();
    
    String illustrationImg;
    try{
      illustrationImg = Utils.getIllustrativeImage(node);
    }catch(Exception ex){
      illustrationImg="";
    }
    String strDateCreated = "";
    if (node.hasProperty(NodetypeConstant.EXO_DATE_CREATED)) {
      Calendar dateCreated = node.getProperty(NodetypeConstant.EXO_DATE_CREATED).getDate();
      strDateCreated = dateFormatter.format(dateCreated.getTime());
    }
    String strLastModified = "";
    if (node.hasNode(NodetypeConstant.JCR_CONTENT)) {
      Node contentNode = node.getNode(NodetypeConstant.JCR_CONTENT);
      if (contentNode.hasProperty(NodetypeConstant.JCR_LAST_MODIFIED)) {
        Calendar lastModified = contentNode.getProperty(NodetypeConstant.JCR_LAST_MODIFIED)
                                           .getDate();
        strLastModified = dateFormatter.format(lastModified.getTime());
      }
    }

    activityOwnerId = activityOwnerId != null ? activityOwnerId : "";

    // populate data to map object
    Map<String, String> activityParams = new HashMap<String, String>();
    activityParams.put(ContentUIActivity.NODE_UUID, node.getUUID());
    activityParams.put(ContentUIActivity.CONTENT_NAME, node.getName());
    activityParams.put(ContentUIActivity.AUTHOR, activityOwnerId);
    activityParams.put(ContentUIActivity.DATE_CREATED, strDateCreated);
    activityParams.put(ContentUIActivity.LAST_MODIFIED, strLastModified);
    activityParams.put(ContentUIActivity.CONTENT_LINK, getContentLink(node));
    activityParams.put(ContentUIActivity.ID,
                       node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE) ? node.getUUID() : "");
    activityParams.put(ContentUIActivity.REPOSITORY, repository);
    activityParams.put(ContentUIActivity.WORKSPACE, workspace);
    activityParams.put(ContentUIActivity.MESSAGE, activityMsgBundleKey);
    activityParams.put(ContentUIActivity.MIME_TYPE, getMimeType(linkManager.isLink(node)?linkManager.getTarget(node, true):node));
    activityParams.put(ContentUIActivity.IMAGE_PATH, illustrationImg);
    activityParams.put(ContentUIActivity.IMAGE_PATH, illustrationImg);
    if (isSystemComment) {
      activityParams.put(ContentUIActivity.IS_SYSTEM_COMMENT, String.valueOf(isSystemComment));
    	activityParams.put(ContentUIActivity.SYSTEM_COMMENT, systemComment);
    }else{
      activityParams.put(ContentUIActivity.IS_SYSTEM_COMMENT, String.valueOf(false));
      activityParams.put(ContentUIActivity.SYSTEM_COMMENT, "");
    }
    activityParams.put(ContentUIActivity.NODE_PATH, node.getPath());
    return activityParams;
  }

  /**
   * see the postActivity(Node node, String activityMsgBundleKey, Boolean isSystemComment, String systemComment)
   */
  public static void postActivity(Node node, String activityMsgBundleKey) throws Exception {
    postActivity(node, activityMsgBundleKey, false, false, null);
  }

  public static ExoSocialActivity createShareActivity(Node node, String activityMsgBundleKey, String activityType, String comments) throws Exception{
    setActivityType(activityType);
    if(SHARE_FILE.equals(activityType)){
      return postFileActivity(node,activityMsgBundleKey,false,false,comments);
    }else if(SHARE_CONTENT.equals(activityType)){
      return postActivity(node,activityMsgBundleKey,false,false,comments);
    }else{
      setActivityType(null);
      return postFileActivity(node,activityMsgBundleKey,false,false,comments);
    }
  }
  /**
   * see the postFileActivity(Node node, String activityMsgBundleKey, Boolean isSystemComment, String systemComment)
   */
  public static void postFileActivity(Node node, String activityMsgBundleKey) throws Exception {
    postFileActivity(node, activityMsgBundleKey, false, false, null);
  }
  
  
  /**
   * 
   * @param node : activity raised from this source
   * @param activityMsgBundleKey
   * @param isSystemComment
   * @param systemComment the new value of System Posted activity, 
   *        if (isSystemComment) systemComment can not be set to null, set to empty string instead of.
   * @throws Exception
   */
  public static ExoSocialActivity postActivity(Node node, String activityMsgBundleKey, boolean needUpdate, 
                                  boolean isSystemComment, String systemComment) throws Exception {
    Object isSkipRaiseAct = DocumentContext.getCurrent()
                                           .getAttributes()
                                           .get(DocumentContext.IS_SKIP_RAISE_ACT);
    if (isSkipRaiseAct != null && Boolean.valueOf(isSkipRaiseAct.toString())) {
      return null;
    }
    if (!isSupportedContent(node)) {
      return null;
    }

    // get services
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ActivityManager activityManager = (ActivityManager) container.getComponentInstanceOfType(ActivityManager.class);
    IdentityManager identityManager = (IdentityManager) container.getComponentInstanceOfType(IdentityManager.class);
    ActivityCommonService activityCommonService =
            (ActivityCommonService)container.getComponentInstanceOfType(ActivityCommonService.class);

    SpaceService spaceService = WCMCoreUtils.getService(SpaceService.class);

    // refine to get the valid node
    refineNode(node);

    // get owner
    String activityOwnerId = getActivityOwnerId(node);
    String nodeActivityID = StringUtils.EMPTY;
    ExoSocialActivity exa =null;
    if (node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
      try {
        nodeActivityID = node.getProperty(ActivityTypeUtils.EXO_ACTIVITY_ID).getString();
        exa =  activityManager.getActivity(nodeActivityID);
      }catch (Exception e){
          LOG.info("No activity is deleted, return no related activity");
      }
    }
    ExoSocialActivity activity = null ;
    String commentID;
    boolean commentFlag = false;
    if (node.isNodeType(MIX_COMMENT) && node.hasProperty(MIX_COMMENT_ID) && activityCommonService.isEditing(node))
    {
      commentID = node.getProperty(MIX_COMMENT_ID).getString();
      if (StringUtils.isNotBlank(commentID)) activity = activityManager.getActivity(commentID);
      commentFlag = (activity != null);
    }
    if (activity==null) {
      String _activityType = StringUtils.isNotEmpty(activityType)?activityType:CONTENT_SPACES;
      activity = createActivity(identityManager, activityOwnerId,
                                node, activityMsgBundleKey, _activityType, isSystemComment, systemComment);
      setActivityType(null);
    }
    
    if (exa!=null) {
      if (commentFlag) {
        Map<String, String> paramsMap = activity.getTemplateParams();
        String paramMessage = paramsMap.get(ContentUIActivity.MESSAGE);
        String paramContent = paramsMap.get(ContentUIActivity.SYSTEM_COMMENT);
        if (!StringUtils.isEmpty(paramMessage)) {
          paramMessage += ActivityCommonService.VALUE_SEPERATOR + activityMsgBundleKey;
          if (StringUtils.isEmpty(systemComment)) {
            paramContent += ActivityCommonService.VALUE_SEPERATOR + " ";
          }else {
            paramContent += ActivityCommonService.VALUE_SEPERATOR + systemComment;
          }
        } else {
          paramMessage = activityMsgBundleKey;
          paramContent = systemComment;
        }
        paramsMap.put(ContentUIActivity.MESSAGE, paramMessage);
        paramsMap.put(ContentUIActivity.SYSTEM_COMMENT, paramContent);
        activity.setTemplateParams(paramsMap);
        updateNotifyMessages(activity, activityMsgBundleKey, systemComment);
        activityManager.updateActivity(activity);
      } else {
        updateNotifyMessages(activity, activity.getTemplateParams().get(ContentUIActivity.MESSAGE), activity.getTemplateParams().get(ContentUIActivity.SYSTEM_COMMENT));
        activityManager.saveComment(exa, activity);
        if (activityCommonService.isEditing(node)) {
          commentID = activity.getId();
          if (node.canAddMixin(MIX_COMMENT)) node.addMixin(MIX_COMMENT);
          if (node.isNodeType(MIX_COMMENT)) node.setProperty(MIX_COMMENT_ID, commentID);
        }
      }
      if (needUpdate) {
        updateMainActivity(activityManager, node, exa);
      }
      return activity;
    }else {
      String spaceGroupName = getSpaceName(node);
      Space space = spaceService.getSpaceByGroupId(SpaceUtils.SPACE_GROUP + "/" + spaceGroupName);
      if (spaceGroupName != null && spaceGroupName.length() > 0
          && space != null) {
        // post activity to space stream
        Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME,
            space.getPrettyName(),
            true);
        activityManager.saveActivityNoReturn(spaceIdentity, activity);
      } else if (activityOwnerId != null && activityOwnerId.length() > 0) {
        // post activity to user status stream
        Identity ownerIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
            activityOwnerId,
            true);
        activityManager.saveActivityNoReturn(ownerIdentity, activity);
      } else {
        return null;
      }
      String activityId = activity.getId();
      if (!StringUtils.isEmpty(activityId)) {
        ActivityTypeUtils.attachActivityId(node, activityId);
      }
      updateMainActivity(activityManager, node, activity);

      if (node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
        try {
          nodeActivityID = node.getProperty(ActivityTypeUtils.EXO_ACTIVITY_ID).getString();
          exa = activityManager.getActivity(nodeActivityID);
        } catch (Exception e) {
          LOG.info("No activity is deleted, return no related activity");
        }
        if (exa != null && !commentFlag && isSystemComment) {
          activityManager.saveComment(exa, activity);
          if (activityCommonService.isEditing(node)) {
            commentID = activity.getId();
            if (node.canAddMixin(MIX_COMMENT)) node.addMixin(MIX_COMMENT);
            if (node.isNodeType(MIX_COMMENT)) node.setProperty(MIX_COMMENT_ID, commentID);
          }
        }
      }

      return activity;
    }
  }
  
  /**
   * 
   * @param node : activity raised from this source
   * @param activityMsgBundleKey
   * @param isSystemComment
   * @param systemComment the new value of System Posted activity, 
   *        if (isSystemComment) systemComment can not be set to null, set to empty string instead of.
   * @throws Exception
   */
  public static ExoSocialActivity postFileActivity(Node node, String activityMsgBundleKey, boolean needUpdate, 
                                  boolean isSystemComment, String systemComment) throws Exception {
    Object isSkipRaiseAct = DocumentContext.getCurrent()
                                           .getAttributes()
                                           .get(DocumentContext.IS_SKIP_RAISE_ACT);
    if (isSkipRaiseAct != null && Boolean.valueOf(isSkipRaiseAct.toString())) {
      return null;
    }
    if (!isSupportedContent(node)) {
      return null;
    }

    // get services
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ActivityManager activityManager = (ActivityManager) container.getComponentInstanceOfType(ActivityManager.class);
    IdentityManager identityManager = (IdentityManager) container.getComponentInstanceOfType(IdentityManager.class);
    ActivityCommonService activityCommonService =
            (ActivityCommonService)container.getComponentInstanceOfType(ActivityCommonService.class);

    SpaceService spaceService = WCMCoreUtils.getService(SpaceService.class);

    // refine to get the valid node
    refineNode(node);

    // get owner
    String activityOwnerId = getActivityOwnerId(node);
    String nodeActivityID = StringUtils.EMPTY;
    ExoSocialActivity exa =null;    
    if (node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
      try {
        nodeActivityID = node.getProperty(ActivityTypeUtils.EXO_ACTIVITY_ID).getString();
        exa =  activityManager.getActivity(nodeActivityID);
      }catch (Exception e){
          LOG.info("No activity is deleted, return no related activity");
      }
    }
    ExoSocialActivity activity = null ;
    String commentID;
    boolean commentFlag = false;
    if (node.isNodeType(MIX_COMMENT) && activityCommonService.isEditing(node)) {
      if (node.hasProperty(MIX_COMMENT_ID)) {
        commentID = node.getProperty(MIX_COMMENT_ID).getString();
        if (StringUtils.isNotBlank(commentID)) activity = activityManager.getActivity(commentID);
        commentFlag = (activity != null);
      }
    }
    if (activity==null) {
      String _activityType = StringUtils.isNotEmpty(activityType)?activityType:FILE_SPACES;
      activity = createActivity(identityManager, activityOwnerId,
                                node, activityMsgBundleKey, _activityType, isSystemComment, systemComment);
      setActivityType(null);
    }
    
    if (exa!=null) {
      if (commentFlag) {
        Map<String, String> paramsMap = activity.getTemplateParams();
        String paramMessage = paramsMap.get(ContentUIActivity.MESSAGE);
        String paramContent = paramsMap.get(ContentUIActivity.SYSTEM_COMMENT);
        if (!StringUtils.isEmpty(paramMessage)) {
          paramMessage += ActivityCommonService.VALUE_SEPERATOR + activityMsgBundleKey;
          if (StringUtils.isEmpty(systemComment)) {
            paramContent += ActivityCommonService.VALUE_SEPERATOR + " ";
          }else {
            paramContent += ActivityCommonService.VALUE_SEPERATOR + systemComment;
          }
        } else {
          paramMessage = activityMsgBundleKey;
          paramContent = systemComment;
        }              
        paramsMap.put(ContentUIActivity.MESSAGE, paramMessage);
        paramsMap.put(ContentUIActivity.SYSTEM_COMMENT, paramContent);
        activity.setTemplateParams(paramsMap);
        updateNotifyMessages(activity, activityMsgBundleKey, systemComment);
        activityManager.updateActivity(activity);
      } else {
        updateNotifyMessages(activity, activity.getTemplateParams().get(ContentUIActivity.MESSAGE), activity.getTemplateParams().get(ContentUIActivity.SYSTEM_COMMENT));
        activityManager.saveComment(exa, activity);
        if (activityCommonService.isEditing(node)) {
          commentID = activity.getId();
          if (node.canAddMixin(MIX_COMMENT)) node.addMixin(MIX_COMMENT);
          if (node.isNodeType(MIX_COMMENT)) node.setProperty(MIX_COMMENT_ID, commentID);
        }
      }      
      return activity;
    }else {
      String spaceGroupName = getSpaceName(node);
      Space space = spaceService.getSpaceByGroupId(SpaceUtils.SPACE_GROUP + "/" + spaceGroupName);
      if (spaceGroupName != null && spaceGroupName.length() > 0
          && space != null) {
        // post activity to space stream
        Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME,
            space.getPrettyName(),
            true);
        activityManager.saveActivityNoReturn(spaceIdentity, activity);
      } else if (activityOwnerId != null && activityOwnerId.length() > 0) {
        // post activity to user status stream
        Identity ownerIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
            activityOwnerId,
            true);
        activityManager.saveActivityNoReturn(ownerIdentity, activity);
      } else {
        return null;
      }
      String activityId = activity.getId();
      if (!StringUtils.isEmpty(activityId)) {
        ActivityTypeUtils.attachActivityId(node, activityId);
      }

      if (node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
        try {
          nodeActivityID = node.getProperty(ActivityTypeUtils.EXO_ACTIVITY_ID).getString();
          exa = activityManager.getActivity(nodeActivityID);
        } catch (Exception e) {
          LOG.info("No activity is deleted, return no related activity");
        }
        if (exa != null && !commentFlag && isSystemComment) {
          activityManager.saveComment(exa, activity);
          if (activityCommonService.isEditing(node)) {
            commentID = activity.getId();
            if (node.canAddMixin(MIX_COMMENT)) node.addMixin(MIX_COMMENT);
            if (node.isNodeType(MIX_COMMENT)) node.setProperty(MIX_COMMENT_ID, commentID);
          }
        }
      }

      return activity;
    }
  }
  
  private static void updateNotifyMessages(ExoSocialActivity activity, String activityMsgBundleKey, String systemComment) 
      throws Exception {     
    Locale locale = new Locale("en");
    ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
    ResourceBundle res = resourceBundleService.getResourceBundle("locale.extension.SocialIntegration", locale);
    StringBuffer sb = new StringBuffer();
    String[] keys = activityMsgBundleKey.split(ActivityCommonService.VALUE_SEPERATOR);
    String[] values = systemComment.split(ActivityCommonService.VALUE_SEPERATOR);
    String message;
    for (String key : keys) {
      try {
        message = res.getString(key);
      } catch(MissingResourceException mre) {
        message = key;
      }
      if(values.length > 0) {
        for(int i = 0; i < values.length; i++) {
          message = message.replace("{"+i+"}", values[i]);
        }
      }
      sb.append(message).append("\n");
    }
    activity.setTitle(sb.toString());
  }
  
  
  private static void updateMainActivity(ActivityManager activityManager, Node contentNode, ExoSocialActivity activity) {
    Map<String, String> activityParams = activity.getTemplateParams();
    String state;
    String nodeTitle;
    String nodeType = null;
    String documentTypeLabel;
    String currentVersion = null;
    TemplateService templateService = WCMCoreUtils.getService(TemplateService.class);
    try {
      nodeType = contentNode.getPrimaryNodeType().getName();
      documentTypeLabel = templateService.getTemplateLabel(nodeType);
    }catch (Exception e) {
      documentTypeLabel = "";
    }
    try {
      nodeTitle = org.exoplatform.ecm.webui.utils.Utils.getTitle(contentNode);
    } catch (Exception e1) {
      nodeTitle ="";
    }
    try {
      state = contentNode.hasProperty(CURRENT_STATE_PROP) ? contentNode.getProperty(CURRENT_STATE_PROP)
          .getValue()
          .getString() : "";
    } catch (Exception e) {
      state="";
    }
    try {
      currentVersion = contentNode.getBaseVersion().getName();
      
      //TODO Must improve this hardcode later, need specification
      if (currentVersion.contains("jcr:rootVersion")) currentVersion = "0";
    }catch (Exception e) {
      currentVersion ="";
    }
    activityParams.put(ContentUIActivity.STATE, state);
    activityParams.put(ContentUIActivity.DOCUMENT_TYPE_LABEL, documentTypeLabel);
    activityParams.put(ContentUIActivity.DOCUMENT_TITLE, nodeTitle);
    activityParams.put(ContentUIActivity.DOCUMENT_VERSION, currentVersion);
    String summary = getSummary(contentNode);
    summary =getFirstSummaryLines(summary, MAX_SUMMARY_LINES_COUNT);
    activityParams.put(ContentUIActivity.DOCUMENT_SUMMARY, summary);
    activity.setTemplateParams(activityParams);
    activityManager.updateActivity(activity);
  }
  /**
   * check the nodes that we support to post activities
   * 
   * @param node for checking
   * @return result of checking
   * @throws RepositoryException
   */
  private static boolean isSupportedContent(Node node) throws Exception {
    if (getActivityOwnerId(node) != null && getActivityOwnerId(node).length() > 0) {
      NodeHierarchyCreator nodeHierarchyCreator = (NodeHierarchyCreator) ExoContainerContext.getCurrentContainer()
                                                                                            .getComponentInstanceOfType(NodeHierarchyCreator.class);
      SessionProvider sessionProvider = WCMCoreUtils.getUserSessionProvider();
      if(sessionProvider == null){
    	  sessionProvider = WCMCoreUtils.getSystemSessionProvider();
      }
      Node userNode = nodeHierarchyCreator.getUserNode(sessionProvider, getActivityOwnerId(node));
      if (userNode != null && node.getPath().startsWith(userNode.getPath() + "/Private/")) {
        return false;
      }
    }

    return true;
  }

  /**
   * refine node for validation
   * 
   * @param currentNode
   * @throws Exception
   */
  private static void refineNode(Node currentNode) throws Exception {
    if (currentNode instanceof NodeImpl && !((NodeImpl) currentNode).isValid()) {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      LinkManager linkManager = (LinkManager) container.getComponentInstanceOfType(LinkManager.class);
      if (linkManager.isLink(currentNode)) {
        try {
          currentNode = linkManager.getTarget(currentNode, false);
        } catch (RepositoryException ex) {
          currentNode = linkManager.getTarget(currentNode, true);
        }
      }
    }
  }

  /**
   * get activity owner
   * 
   * @return activity owner
   */
  private static String getActivityOwnerId(Node node) {
    String activityOwnerId = "";
    ConversationState conversationState = ConversationState.getCurrent();
    if (conversationState != null) {
      activityOwnerId = conversationState.getIdentity().getUserId();
    }else{
      try {
        activityOwnerId = node.getProperty("publication:lastUser").getString();
      } catch (Exception e) {
        LOG.info("No lastUser publication");
      }	
    }
    return activityOwnerId;
  }
  
  /**
   * get the space name of node
   * 
   * @param node
   * @return the group name
   * @throws Exception
   */
  private static String getSpaceName(Node node) throws Exception {
    NodeHierarchyCreator nodeHierarchyCreator = (NodeHierarchyCreator) ExoContainerContext.getCurrentContainer()
                                                                                          .getComponentInstanceOfType(NodeHierarchyCreator.class);
    String groupPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
    String spacesFolder = groupPath + "/spaces/";
    String spaceName = "";
    String nodePath = node.getPath();
    if (nodePath.startsWith(spacesFolder)) {
      spaceName = nodePath.substring(spacesFolder.length());
      spaceName = spaceName.substring(0, spaceName.indexOf("/"));
    }

    return spaceName;
  }

  /**
   * Generate the viewer link to site explorer by node
   * 
   * @param node the node
   * @return String the viewer link
   * @throws RepositoryException
   */
  public static String getContentLink(Node node) throws Exception {
    DocumentService documentService = CommonsUtils.getService(DocumentService.class);
    return documentService.getLinkInDocumentsApp(node.getPath());
  }

  /**
   * Create ExoSocialActivity
   * 
   * @param identityManager the identity Manager
   * @param activityOwnerId the remote user name
   * @param node the node
   * @param activityMsgBundleKey the message bundle key
   * @param activityType the activity type
   * @return the ExoSocialActivity
   * @throws Exception the activity storage exception
   */
  public static ExoSocialActivity createActivity(IdentityManager identityManager,
                                                 String activityOwnerId, Node node,
                                                 String activityMsgBundleKey, String activityType) throws Exception {
	  return createActivity(identityManager, activityOwnerId, node, activityMsgBundleKey, activityType, false, null);
  }
  public static ExoSocialActivity createActivity(IdentityManager identityManager,
                                                 String activityOwnerId,
                                                 Node node, String activityMsgBundleKey, String activityType,
                                                 boolean isSystemComment,  String systemComment) throws Exception {
		// Populate activity data
	Map<String, String> activityParams = populateActivityData(node, activityOwnerId, activityMsgBundleKey, isSystemComment, systemComment);
	
    String title = node.hasProperty(NodetypeConstant.EXO_TITLE) ? node.getProperty(NodetypeConstant.EXO_TITLE)
                                                                      .getString()
                                                               : org.exoplatform.ecm.webui.utils.Utils.getTitle(node);
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    String userId = "";
    if(ConversationState.getCurrent() != null)
    {
      userId = ConversationState.getCurrent().getIdentity().getUserId();
    }else{
      userId = activityOwnerId;
    }
    Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, 
      userId, false);
    activity.setUserId(identity.getId());
    activity.setType(activityType);
    activity.setUrl(node.getPath());
    if(StringUtils.isNotEmpty(activityMsgBundleKey) && StringUtils.isNotEmpty(systemComment)) {
      updateNotifyMessages(activity, activityMsgBundleKey, systemComment);
    } else if(StringUtils.isNotEmpty(systemComment)){
        activity.setTitle(systemComment);
    } else {
        activity.setTitle(title);
    }
    activity.setTemplateParams(activityParams);
    return activity;
  }
  
  public static void deleteFileActivity(Node node) throws RepositoryException {
    // get services
    ExoContainer container = ExoContainerContext.getCurrentContainer();
    ActivityManager activityManager = (ActivityManager) container.getComponentInstanceOfType(ActivityManager.class);
    
    // get owner
    String nodeActivityID = StringUtils.EMPTY;
    if (node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
      try {
        nodeActivityID = node.getProperty(ActivityTypeUtils.EXO_ACTIVITY_ID).getString();
        if(activityManager.getActivity(nodeActivityID) != null) {
          activityManager.deleteActivity(nodeActivityID);
        }
      } catch (Exception e) {
        LOG.info("No activity is deleted, return no related activity");
      }
    }    
  }

  /**
   * Gets the illustrative image.
   * 
   * @param node the node
   * @return the illustrative image
   */
  public static String getIllustrativeImage(Node node) {
    WebSchemaConfigService schemaConfigService = WCMCoreUtils.getService(WebSchemaConfigService.class);
    WebContentSchemaHandler contentSchemaHandler = schemaConfigService.getWebSchemaHandlerByType(WebContentSchemaHandler.class);
    Node illustrativeImage = null;
    String uri = "";
    try {
      illustrativeImage = contentSchemaHandler.getIllustrationImage(node);
      uri = generateThumbnailImageURI(illustrativeImage);
    } catch (PathNotFoundException ex) {
      return uri;
    } catch (Exception e) { // WebContentSchemaHandler
      LOG.warn(e.getMessage(), e);
    }
    return uri;
  }

  /**
   * Generate the Thumbnail Image URI.
   * 
   * @param file the node
   * @return the Thumbnail uri with medium size
   * @throws Exception the exception
   */
  public static String generateThumbnailImageURI(Node file) throws Exception {
    StringBuilder builder = new StringBuilder();
    NodeLocation fielLocation = NodeLocation.getNodeLocationByNode(file);
    String repository = fielLocation.getRepository();
    String workspaceName = fielLocation.getWorkspace();
    String nodeIdentifiler = file.getPath().replaceFirst("/", "");
    String portalName = PortalContainer.getCurrentPortalContainerName();
    String restContextName = PortalContainer.getCurrentRestContextName();
    InputStream stream = file.getNode(NodetypeConstant.JCR_CONTENT)
                             .getProperty(NodetypeConstant.JCR_DATA)
                             .getStream();
    if (stream.available() == 0)
      return null;
    stream.close();
    builder.append("/")
           .append(portalName)
           .append("/")
           .append(restContextName)
           .append("/")
           .append("thumbnailImage/medium/")
           .append(repository)
           .append("/")
           .append(workspaceName)
           .append("/")
           .append(nodeIdentifiler);
    return builder.toString();
  }

  /**
   * Get the MimeType
   * 
   * @param node the node
   * @return the MimeType
   */
  public static String getMimeType(Node node) {
    try {
      if (node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
        if (node.hasNode(NodetypeConstant.JCR_CONTENT))
          return node.getNode(NodetypeConstant.JCR_CONTENT)
                     .getProperty(NodetypeConstant.JCR_MIME_TYPE)
                     .getString();
      }
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    }
    return "";
  }
  
  public static String getSummary(Node node) {
    String desc = "";
    try {
      if (node != null) {
        if (node.hasProperty("exo:summary")) {
          desc = node.getProperty("exo:summary").getValue().getString();
        } else if (node.hasNode("jcr:content")) {
          Node content = node.getNode("jcr:content");
          if (content.hasProperty("dc:description") && content.getProperty("dc:description").getValues().length > 0) {
            desc = content.getProperty("dc:description").getValues()[0].getString();
          }
        }
      }
    } catch (RepositoryException re) {
      if (LOG.isWarnEnabled())
        LOG.warn("RepositoryException: ", re);
    }
    return desc;
  }
  public static String getFirstSummaryLines(String source) {
    return getFirstSummaryLines(source, MAX_SUMMARY_LINES_COUNT);
  }
  
  
  private static String convertActivityContent(String source){
    String result =  source;
    result = result.replaceAll("(?i)<head>.*</head>", "");
    result = result.replaceAll("(?i)<script.*>.*</script>", "");
    result = result.replaceAll("(?i)<style.*>.*</style>", "");
    result = result.replaceAll("<([a-zA-Z\"]+) *[^/]*?>", "");
    result = result.replaceAll("</p>", "<br>");
    result = result.replaceAll("</([a-zA-Z]+) *[^/]*?>", "");
    result = result.replaceAll("([\r\n\t])+", "");
    result = result.replaceAll("^(<br>)", "");
    result = result.replaceAll("(<br>[ \r\t\n]+<br>)", "\n");
    result = result.replaceAll("(<br>)+", "\n");
    return result;
  }
  
  /**
   * 
   * @param source
   * @param linesCount
   * @return first {@code linesCount} without HTML tag
   */
  public static String getFirstSummaryLines(String source, int linesCount) {
    String result =  convertActivityContent(source);
    int i = 0;
    int index = -1;
    while (true) {
      index = result.indexOf("\n", index+1);
      if (index<0) break;
      i++;
      if (i>=linesCount) break;
    }
    if (index <0) {
      if (result.length()>MAX_SUMMARY_CHAR_COUNT)
      return  result.substring(0, MAX_SUMMARY_CHAR_COUNT-1) + "...";
      return result;
    }
    if (index>MAX_SUMMARY_CHAR_COUNT) index = MAX_SUMMARY_CHAR_COUNT-1;
    result = result.substring(0, index) + "\n...";
    return result;
  }

  public static String[] getSystemCommentTitle(Map<String, String> activityParams) {
    String[] result;
    if (activityParams == null) return null;
    String commentValue = activityParams.get(FileUIActivity.SYSTEM_COMMENT);
    if (!StringUtils.isEmpty(commentValue)) {
      if (commentValue.indexOf(ActivityCommonService.VALUE_SEPERATOR) >= 0) {
        result = commentValue.split(ActivityCommonService.VALUE_SEPERATOR);
        return result;
      } else {
        return new String[]{commentValue};
      }
    }
    return null;
  }

  public static String[] getSystemCommentBundle(Map<String, String> activityParams) {
    String[] result;
    if (activityParams == null) return null;
    String tmp = activityParams.get(FileUIActivity.IS_SYSTEM_COMMENT);
    String commentMessage;
    if (tmp == null) return null;
    try {
      if (Boolean.parseBoolean(tmp)) {
        commentMessage = activityParams.get(FileUIActivity.MESSAGE);
        if (!StringUtils.isEmpty(commentMessage)) {
          if (commentMessage.indexOf(ActivityCommonService.VALUE_SEPERATOR) >= 0) {
            result = commentMessage.split(ActivityCommonService.VALUE_SEPERATOR);
            return result;
          } else {
            return new String[]{commentMessage};
          }
        }
      }
    } catch (Exception e) {
      return null;
    }
    return null;
  }

  public static String getBundleValue(String key) {
    try {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      ResourceBundle res = context.getApplicationResourceBundle();
      String value = res.getString(key);
      return value;
    } catch (MissingResourceException e) {
      return key;
    }
  }
}
