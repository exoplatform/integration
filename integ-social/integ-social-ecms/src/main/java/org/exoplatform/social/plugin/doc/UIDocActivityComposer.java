/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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

package org.exoplatform.social.plugin.doc;

import static org.exoplatform.social.plugin.doc.UIDocActivityBuilder.ACTIVITY_TYPE;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.webui.selector.UISelectable;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.social.core.BaseActivityProcessorPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.webui.composer.PopupContainer;
import org.exoplatform.social.webui.composer.UIActivityComposer;
import org.exoplatform.social.webui.composer.UIActivityComposerManager;
import org.exoplatform.social.webui.composer.UIComposer;
import org.exoplatform.social.webui.composer.UIComposer.PostContext;
import org.exoplatform.social.webui.profile.UIUserActivitiesDisplay;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.UIPortletApplication;
import org.exoplatform.webui.cssfile.CssClassUtils;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormStringInput;

/**
 * The templateParamsProcessor to process an activity. Replace template
 * key by template value in activity's title.
 * @author    Zun
 * @since     Apr 19, 2010
 * @copyright eXo Platform SAS
 */
@ComponentConfig(
  template = "classpath:groovy/social/plugin/doc/UIDocActivityComposer.gtmpl",
  events = {
    @EventConfig(listeners = UIActivityComposer.CloseActionListener.class),
    @EventConfig(listeners = UIActivityComposer.SubmitContentActionListener.class),
    @EventConfig(listeners = UIActivityComposer.ActivateActionListener.class),
    @EventConfig(listeners = UIDocActivityComposer.SelectDocumentActionListener.class),
    @EventConfig(listeners = UIDocActivityComposer.RemoveDocumentActionListener.class)
  }
)
public class UIDocActivityComposer extends UIActivityComposer implements UISelectable {
  public static final String REPOSITORY = "repository";
  public static final String WORKSPACE = "collaboration";
  private static final String FILE_SPACES = "files:spaces";
  private final static String POPUP_COMPOSER = "UIPopupComposer";
  private final String docActivityTitle = "<a href=\"${"+ UIDocActivity.DOCLINK +"}\">" + "${" +UIDocActivity.DOCNAME +"}</a>";

  private String documentRefLink;
  private String documentPath;
  private String documentName;
  private boolean isDocumentReady;
  private String currentUser;
  private String docIcon = "";
  
  /**
   * constructor
   */
  public UIDocActivityComposer() {
    addChild(new UIFormStringInput("InputDoc", "InputDoc", null));
    resetValues();
  }

  private void resetValues() {
    documentRefLink = "";
    isDocumentReady = false;
    setReadyForPostingActivity(false);
  }

  public boolean isDocumentReady() {
    return isDocumentReady;
  }

  public String getDocumentName() {
    return documentName;
  }

  public String getDocumentIcon() {
    return docIcon;
  }

  /**
   * @return the currentUser
   */
  public String getCurrentUser() {
    return currentUser;
  }

  /**
   * @param currentUser the currentUser to set
   */
  public void setCurrentUser(String currentUser) {
    this.currentUser = currentUser;
  }

  @Override
  protected void onActivate(Event<UIActivityComposer> event) {
    isDocumentReady = false;
    setCurrentUser(event.getRequestContext().getRemoteUser());
  }

  @Override
  protected void onClose(Event<UIActivityComposer> event) {
    resetValues();
  }

  @Override
  protected void onSubmit(Event<UIActivityComposer> event) {
  }
  
  @Override
  public void onPostActivity(PostContext postContext, UIComponent source,
                             WebuiRequestContext requestContext, String postedMessage) throws Exception {
  }

  @Override
  public ExoSocialActivity onPostActivity(PostContext postContext, String postedMessage) throws Exception {
    ExoSocialActivity activity = null;
    if(!isDocumentReady){
      getAncestorOfType(UIPortletApplication.class)
        .addMessage(new ApplicationMessage("UIComposer.msg.error.Must_select_file", null, ApplicationMessage.INFO));
    } else {
      Map<String, String> activityParams = new LinkedHashMap<String, String>();
      Node node = getDocNode(REPOSITORY, WORKSPACE, documentPath);
      activityParams.put(UIDocActivity.DOCUMENT_TITLE, Utils.getTitle(node));

      boolean isSymlink = node.isNodeType(NodetypeConstant.EXO_SYMLINK);
      if (isSymlink){
        node = Utils.getNodeSymLink(node);
      }

      activityParams.put(UIDocActivity.IS_SYMLINK, String.valueOf(isSymlink));
      activityParams.put(UIDocActivity.DOCNAME, documentName);
      activityParams.put(UIDocActivity.DOCLINK, documentRefLink);
      activityParams.put(UIDocActivity.DOCPATH, documentPath);
      activityParams.put(UIDocActivity.REPOSITORY, REPOSITORY);
      activityParams.put(UIDocActivity.WORKSPACE, WORKSPACE);
      activityParams.put(UIDocActivity.MESSAGE, postedMessage);      
      activityParams.put(BaseActivityProcessorPlugin.TEMPLATE_PARAM_TO_PROCESS, UIDocActivity.MESSAGE);
      
      if(node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)
              || node.isNodeType(NodetypeConstant.EXO_ACCESSIBLE_MEDIA)) {
        String activityOwnerId = UIDocActivity.getActivityOwnerId(node);
        DateFormat dateFormatter = null;
        dateFormatter = new SimpleDateFormat(ISO8601.SIMPLE_DATETIME_FORMAT);
        
        String illustrationImg = UIDocActivity.getIllustrativeImage(node);
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

        activityParams.put(UIDocActivity.ID, node.isNodeType(NodetypeConstant.MIX_REFERENCEABLE) ? node.getUUID() : "");
        activityParams.put(UIDocActivity.CONTENT_NAME, node.getName());
        activityParams.put(UIDocActivity.AUTHOR, activityOwnerId);
        activityParams.put(UIDocActivity.DATE_CREATED, strDateCreated);
        activityParams.put(UIDocActivity.LAST_MODIFIED, strLastModified);
        activityParams.put(UIDocActivity.CONTENT_LINK, UIDocActivity.getContentLink(node));
        activityParams.put(UIDocActivity.MIME_TYPE, UIDocActivity.getMimeType(node));
        activityParams.put(UIDocActivity.IMAGE_PATH, illustrationImg);
      }
      //
      if(postContext == UIComposer.PostContext.SPACE){
        activity = postActivityToSpace(activityParams);
      } else if (postContext == UIComposer.PostContext.USER){
        activity = postActivityToUser(activityParams);
      }
    }
    resetValues();
    return activity;
  }

  private ExoSocialActivity postActivityToUser(Map<String, String> activityParams) throws Exception {
    String ownerName = ((UIUserActivitiesDisplay) getActivityDisplay()).getOwnerName();
    IdentityManager identityManager = getApplicationComponent(IdentityManager.class);
    Identity ownerIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, ownerName, true);
    //
    return saveActivity(activityParams, identityManager, ownerIdentity);
  }

  private ExoSocialActivity postActivityToSpace(Map<String, String> activityParams) throws Exception {
    Space space = getApplicationComponent(SpaceService.class).getSpaceByUrl(SpaceUtils.getSpaceUrlByContext());
    IdentityManager identityManager = getApplicationComponent(IdentityManager.class);
    Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(),false);
    //
    return saveActivity(activityParams, identityManager, spaceIdentity);
  }

  public void doSelect(String selectField, Object value) throws Exception {
    String rawPath = value.toString();
    rawPath = rawPath.substring(rawPath.indexOf(":/") + 2);
    documentRefLink = buildDocumentLink(rawPath);
    documentName = rawPath.substring(rawPath.lastIndexOf("/") + 1);
    documentPath = buildDocumentPath(rawPath);
    isDocumentReady = true;
    documentRefLink = documentRefLink.replace("//", "/");
    documentPath = documentPath.replace("//", "/");
    docIcon = CssClassUtils.getCSSClassByFileNameAndFileType(documentName, selectField, null);
    setReadyForPostingActivity(true);
    UIActivityComposer activityComposer = getActivityComposerManager().getCurrentActivityComposer();
    activityComposer.setDisplayed(true);
  }  

  private ExoSocialActivity saveActivity(Map<String, String> activityParams, IdentityManager identityManager, Identity ownerIdentity) throws RepositoryException {
    Node node = getDocNode(activityParams.get(UIDocActivity.REPOSITORY), activityParams.get(UIDocActivity.WORKSPACE), 
                           activityParams.get(UIDocActivity.DOCPATH));
    String activity_type = ACTIVITY_TYPE;
    if(node.getPrimaryNodeType().getName().equals(NodetypeConstant.NT_FILE)) {
      activity_type = FILE_SPACES;
    }
    String remoteUser = ConversationState.getCurrent().getIdentity().getUserId();
    Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, remoteUser, true);
    String title = activityParams.get(UIDocActivity.MESSAGE);
    if (title == null || title.length() == 0) {
      title = docActivityTitle;
    }
    ExoSocialActivity activity = new ExoSocialActivityImpl(userIdentity.getId(), activity_type, title, null);
    activity.setTemplateParams(activityParams);
    //
    ActivityManager activityManager = getApplicationComponent(ActivityManager.class);
    activityManager.saveActivityNoReturn(ownerIdentity, activity);
    
    String activityId = activity.getId();
    if (!StringUtils.isEmpty(activityId)) {
      ActivityTypeUtils.attachActivityId(node, activityId);
      node.save();
    }
    //
    return activityManager.getActivity(activity.getId());
  }

  private String buildDocumentLink(String rawPath) {
    String portalContainerName = PortalContainer.getCurrentPortalContainerName();
    String restContextName = PortalContainer.getCurrentRestContextName();
    String restService = "jcr";
    return new StringBuilder().append("/").append(portalContainerName)
                                            .append("/").append(restContextName)
                                            .append("/").append(restService)
                                            .append("/").append(REPOSITORY)
                                            .append("/").append(WORKSPACE)
                                            .append("/").append(rawPath).toString();
  }

  public String buildDocumentPath(String rawPath){
    return "/" + rawPath;
  }

  public static class SelectDocumentActionListener  extends EventListener<UIDocActivityComposer> {
    @Override
    public void execute(Event<UIDocActivityComposer> event) throws Exception {
      UIDocActivityComposer docActivityComposer = event.getSource();
      PopupContainer popupContainer = docActivityComposer.getAncestorOfType(UIPortletApplication.class)
                                                          .findFirstComponentOfType(PopupContainer.class);
      popupContainer.activate(UIDocActivitySelector.class, 500, POPUP_COMPOSER);
      event.getRequestContext().addUIComponentToUpdateByAjax(popupContainer);
    }
  }
  
  public static class RemoveDocumentActionListener  extends EventListener<UIDocActivityComposer> {
    @Override
    public void execute(Event<UIDocActivityComposer> event) throws Exception {
      final UIDocActivityComposer docActivityComposer = event.getSource();
      final UIActivityComposerManager activityComposerManager = docActivityComposer.getActivityComposerManager();
      final UIActivityComposer activityComposer = activityComposerManager.getCurrentActivityComposer();
      activityComposerManager.setDefaultActivityComposer();
      activityComposer.setDisplayed(false);
      // Reset values
      docActivityComposer.resetValues();
    }
  }
  
  protected Node getDocNode(String repository, String workspace, String docPath) {
    NodeLocation nodeLocation = new NodeLocation(repository, workspace, docPath);
    return NodeLocation.getNodeByLocation(nodeLocation);
  }
  
}
