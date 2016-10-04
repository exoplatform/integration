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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.utils.permission.PermissionUtil;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wcm.ext.component.document.service.IShareDocumentService;
import org.exoplatform.wcm.notification.plugin.ShareFileToSpacePlugin;
import org.exoplatform.wcm.notification.plugin.ShareFileToUserPlugin;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.web.application.RequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.cssfile.CssClassIconFile;
import org.exoplatform.webui.cssfile.CssClassManager;
import org.exoplatform.webui.cssfile.CssClassUtils;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.*;

import static org.exoplatform.wcm.notification.plugin.FileActivityChildPlugin.EXO_RESOURCES_URI;
import static org.exoplatform.wcm.notification.plugin.FileActivityChildPlugin.ICON_FILE_EXTENSION;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 18, 2014 
 */
@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template =  "classpath:groovy/ecm/social-integration/share-document/UIShareDocuments.gtmpl",
    events = {
        @EventConfig(listeners = UIShareDocuments.ConfirmActionListener.class),
        @EventConfig(listeners = UIShareDocuments.CancelActionListener.class),
        @EventConfig(listeners = UIShareDocuments.TextChangeActionListener.class),
        @EventConfig(listeners = UIShareDocuments.ChangeActionListener.class),
        @EventConfig(listeners = UIShareDocuments.AddActionListener.class),
        @EventConfig(listeners = UIShareDocuments.ChangePermissionActionListener.class)
    }
)
public class UIShareDocuments extends UIForm implements UIPopupComponent{

  private static final Log    LOG                 = ExoLogger.getLogger(UIShareDocuments.class);
  private static final String SHARECONTENT_BUNDLE_LOCATION = "locale.extension.SocialIntegration";
  private static final String SHARE_OPTION_CANVEW          = "UIShareDocuments.label.option.read";
  private static final String SHARE_OPTION_CANMODIFY       = "UIShareDocuments.label.option.modify";
  private static final String SHARE_PERMISSION_VIEW        = PermissionType.READ;
  private static final String SHARE_PERMISSION_MODIFY      = "modify";
  private static final String SPACE_PREFIX1 = "space::";
  private static final String SPACE_PREFIX2 = "*:/spaces/";


  private String permission = SHARE_PERMISSION_VIEW;
  private boolean permDropDown = false;

  public boolean hasPermissionDropDown() {
    return permDropDown;
  }

  public void setPermissionDropDown(boolean permDropDown) {
    this.permDropDown = permDropDown;
  }

  public void removePermission(String id) {
    this.permissions.remove(id);
  }

  public void updatePermission(String id, String permission) {
    this.permissions.remove(id);
    this.permissions.put(id,permission);
  }


  public static class ChangeActionListener extends EventListener<UIShareDocuments> {

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      String permission = "read";
      UIShareDocuments uiform = event.getSource();
      if (uiform.getChild(UIFormSelectBox.class).getValue().equals(SHARE_PERMISSION_MODIFY)) {
        uiform.getChild(UIFormSelectBox.class).setValue(SHARE_PERMISSION_VIEW);
      } else {
        uiform.getChild(UIFormSelectBox.class).setValue(SHARE_PERMISSION_MODIFY);
        permission = SHARE_PERMISSION_MODIFY;
      }
      UIWhoHasAccess uiWhoHasAccess = uiform.getParent();
      uiWhoHasAccess.updateEntry(uiform.getId(), permission);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiform);
    }
  }

  public static class CancelActionListener extends EventListener<UIShareDocuments>{

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      event.getSource().getAncestorOfType(UIJCRExplorer.class).cancelAction() ;
    }
  }
  public static class TextChangeActionListener extends EventListener<UIShareDocuments>{

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      UIShareDocuments uiform = event.getSource();
      uiform.comment = event.getSource().getChild(UIFormTextAreaInput.class).getValue();
      event.getRequestContext().addUIComponentToUpdateByAjax(event.getSource().getChild(UIFormTextAreaInput.class));
    }
  }

  public static class ConfirmActionListener extends EventListener<UIShareDocuments>{

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {

      UIShareDocuments uiform = event.getSource();
      IShareDocumentService service = WCMCoreUtils.getService(IShareDocumentService.class);
      SpaceService spaceService = WCMCoreUtils.getService(SpaceService.class);
      List<String> entries = uiform.entries;
      Map<String,String> permissions = uiform.permissions;
      Set<String> accessList = uiform.getWhoHasAccess();
      Node node = event.getSource().getNode();
      String message = "";
      String perm = "read";
      String user = ConversationState.getCurrent().getIdentity().getUserId();
      if (uiform.isOwner(user) || uiform.canEdit(user)) {
        if (event.getSource().getChild(UIFormTextAreaInput.class).getValue() != null)
          message = event.getSource().getChild(UIFormTextAreaInput.class).getValue();
        for (String name : accessList) {
          try {
            if ((permissions.containsKey(name) && permissions.get(name).equals(uiform.getPermission(name)))
              || uiform.isOwner(name)) continue;
                      else if (permissions.containsKey(name)) {
              if (!name.startsWith(SPACE_PREFIX2)) {
                service.unpublishDocumentToUser(name, (ExtendedNode) node);
                service.publishDocumentToUser(name, node, message, permissions.get(name));
                NotificationContext ctx = NotificationContextImpl.cloneInstance().append(ShareFileToUserPlugin.NODE, node)
                    .append(ShareFileToUserPlugin.SENDER, ConversationState.getCurrent().getIdentity().getUserId())
                    .append(ShareFileToUserPlugin.NODEID, node.getUUID())
                    .append(ShareFileToUserPlugin.URL, getDocumentUrl(node))
                    .append(ShareFileToUserPlugin.RECEIVER, name)
                    .append(ShareFileToUserPlugin.PERM, permissions.get(name))
                    .append(ShareFileToUserPlugin.ICON, uiform.getDefaultThumbnail(node))
                    .append(ShareFileToUserPlugin.MIMETYPE, uiform.getMimeType(node))
                    .append(ShareFileToUserPlugin.MESSAGE, message);
                ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(ShareFileToUserPlugin.ID))).execute(ctx);
              } else {
                String groupId = name.substring("*:".length());
                service.unpublishDocumentToSpace(groupId, (ExtendedNode) node);
                String activityId = service.publishDocumentToSpace(groupId, node, message, permissions.get(name));
                NotificationContext ctx = NotificationContextImpl.cloneInstance().append(ShareFileToSpacePlugin.NODE, node)
                    .append(ShareFileToSpacePlugin.SENDER, ConversationState.getCurrent().getIdentity().getUserId())
                    .append(ShareFileToSpacePlugin.NODEID, node.getUUID())
                    .append(ShareFileToUserPlugin.URL, getDocumentUrl(node))
                    .append(ShareFileToSpacePlugin.RECEIVER, groupId)
                    .append(ShareFileToSpacePlugin.PERM, permissions.get(name))
                    .append(ShareFileToSpacePlugin.ICON, uiform.getDefaultThumbnail(node))
                    .append(ShareFileToSpacePlugin.MIMETYPE, uiform.getMimeType(node))
                    .append(ShareFileToSpacePlugin.ACTIVITY_ID, activityId)
                    .append(ShareFileToSpacePlugin.MESSAGE, message);
                ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(ShareFileToSpacePlugin.ID))).execute(ctx);
              }
            } else if (!name.startsWith(SPACE_PREFIX2)) {
              service.unpublishDocumentToUser(name, (ExtendedNode) node);
            } else {
              String groupId = name.substring("*:".length());
              service.unpublishDocumentToSpace(groupId, (ExtendedNode) node);
            }
          } catch (RepositoryException e) {
            UIShareDocuments uicomp = event.getSource() ;
            UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
            uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.InvalidEntry", null,
                ApplicationMessage.WARNING));
          }
        }
        if (entries.size() > 0) {
          for (String entry : entries) {
            if (entry.equals("") || uiform.isOwner(entry)) continue;
            else {
              perm = permissions.get(entry);
              String activityId = "";
              if (entry.startsWith(SPACE_PREFIX2)) {
                String groupId = spaceService.getSpaceByPrettyName(entry.substring(SPACE_PREFIX2.length())).getGroupId();
                activityId = service.publishDocumentToSpace(groupId, node, message, perm);
                NotificationContext ctx = NotificationContextImpl.cloneInstance().append(ShareFileToSpacePlugin.NODE, node)
                    .append(ShareFileToSpacePlugin.SENDER, ConversationState.getCurrent().getIdentity().getUserId())
                    .append(ShareFileToSpacePlugin.NODEID, node.getUUID())
                    .append(ShareFileToUserPlugin.URL, getDocumentUrl(node))
                    .append(ShareFileToSpacePlugin.RECEIVER, groupId)
                    .append(ShareFileToSpacePlugin.PERM, perm)
                    .append(ShareFileToSpacePlugin.ICON, uiform.getDefaultThumbnail(node))
                    .append(ShareFileToSpacePlugin.MIMETYPE, uiform.getMimeType(node))
                    .append(ShareFileToSpacePlugin.ACTIVITY_ID, activityId)
                    .append(ShareFileToSpacePlugin.MESSAGE, message);
                ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(ShareFileToSpacePlugin.ID))).execute(ctx);
              } else {
                service.publishDocumentToUser(entry, node, message, perm);
                NotificationContext ctx = NotificationContextImpl.cloneInstance().append(ShareFileToUserPlugin.NODE, node)
                    .append(ShareFileToUserPlugin.SENDER, ConversationState.getCurrent().getIdentity().getUserId())
                    .append(ShareFileToUserPlugin.NODEID, node.getUUID())
                    .append(ShareFileToUserPlugin.URL, getDocumentUrl(node))
                    .append(ShareFileToUserPlugin.RECEIVER, entry)
                    .append(ShareFileToUserPlugin.PERM, permissions.get(entry))
                    .append(ShareFileToUserPlugin.ICON, uiform.getDefaultThumbnail(node))
                    .append(ShareFileToUserPlugin.MIMETYPE, uiform.getMimeType(node))
                    .append(ShareFileToUserPlugin.MESSAGE, message);

                ctx.getNotificationExecutor().with(ctx.makeCommand(PluginKey.key(ShareFileToUserPlugin.ID))).execute(ctx);
              }
            }
          }
        }
        event.getSource().getAncestorOfType(UIJCRExplorer.class).cancelAction();
      } else {
        UIShareDocuments uicomp = event.getSource() ;
        UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.NoPermission", null,
            ApplicationMessage.WARNING));
      }
    }

    private String getDocumentUrl(Node node) {
      DocumentService docServ = WCMCoreUtils.getService(DocumentService.class);
      try {
        return CommonsUtils.getCurrentDomain()
            + docServ.getShortLinkInDocumentsApp(node.getSession().getWorkspace().getName(), node.getUUID());
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
        return "";
      }
    }
  }

  public static class AddActionListener extends EventListener<UIShareDocuments> {

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      UIShareDocuments uicomponent = event.getSource();
      List<String> entries = event.getSource().entries;
      UIFormStringInput input = uicomponent.getUIStringInput(USER);
      String value = input.getValue();
      input.setValue(null);
      if (value == null || value.trim().isEmpty()) {
        UIShareDocuments uicomp = event.getSource() ;
        UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.NoSpace", null,
            ApplicationMessage.WARNING)) ;
      }
      if (value != null) {
        String[] selectedIdentities = value.split(",");
        String name = null;
        String user = ConversationState.getCurrent().getIdentity().getUserId();
        if (uicomponent.hasPermissionDropDown() && (uicomponent.canEdit(user) || uicomponent.isOwner(user))) {
          String permission = uicomponent.getPermission();
          List<String> notFound = new LinkedList<String>();
          int i=0;
          if (selectedIdentities != null) {
            for (int idx = 0; idx < selectedIdentities.length; idx++) {
              name = selectedIdentities[idx].trim();
              if (name.length() > 0) {
                if (isExisting(name) && !uicomponent.isOwner(name)) {
                  if (name.startsWith(SPACE_PREFIX1)) name = name.replace(SPACE_PREFIX1, SPACE_PREFIX2);
                  if (!uicomponent.hasPermission(name, permission)) {
                    if (uicomponent.permissions.containsKey(name)) uicomponent.permissions.remove(name);
                    uicomponent.permissions.put(name, permission);
                    uicomponent.getChild(UIWhoHasAccess.class).update(name, permission);
                    if (!entries.contains(name)) entries.add(name);
                  }
                } else if (uicomponent.isOwner(name)) {
                  UIShareDocuments uicomp = event.getSource() ;
                  UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
                  uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.InvalidOwner", null,
                          ApplicationMessage.WARNING)) ;
                } else {
                  notFound.add(name);
                }
              }
            }
          }
          if (notFound.size() > 0) {
            UIShareDocuments uicomp = event.getSource() ;
            UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
            uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.Invalid", new String[]{notFound.toString()},
                ApplicationMessage.WARNING)) ;
          }
          event.getRequestContext().addUIComponentToUpdateByAjax(uicomponent);
          event.getRequestContext().getJavascriptManager()
              .require("SHARED/share-content", "shareContent")
              .addScripts("eXo.ecm.ShareContent.checkSelectedEntry('" + entries + "');");
        } else {
          UIShareDocuments uicomp = event.getSource() ;
          UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
          uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.NoPermission", null,
              ApplicationMessage.WARNING));
        }
      }
    }
  }

  public static class ChangePermissionActionListener extends EventListener<UIShareDocuments> {

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      UIShareDocuments uicomponent = event.getSource();
      if (uicomponent.getPermission().equals(SHARE_PERMISSION_MODIFY)) uicomponent.setPermission(SHARE_PERMISSION_VIEW);
      else uicomponent.setPermission(SHARE_PERMISSION_MODIFY);
      event.getRequestContext().addUIComponentToUpdateByAjax(uicomponent);
    }
  }

  private void setPermission(String permission) {
    this.permission = permission;
  }

  private String getPermission() {
    return permission;
  }

  private static boolean isExisting(String name) {
    if (name.contains("space::")) {
      SpaceService service = WCMCoreUtils.getService(SpaceService.class);
      return (service.getSpaceByPrettyName(name.split("::")[1]) != null);
    } else {
      ExoContainer container = ExoContainerContext.getCurrentContainer();
      IdentityManager identityManager = (IdentityManager) container.getComponentInstanceOfType(IdentityManager.class);
      return (identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, name, true) != null);
    }
  }

  private boolean hasPermission(String name, String permission) {
    if (permissions.containsKey(name)) {
      return permissions.get(name).equals(permission);
    }
    return false;
  }

  private String nodePath;
  List<String> entries = new ArrayList<String>();
  public String comment = "";
  private NodeLocation node;
  private static final String USER = "user";
  private Map<String, String> permissions;

  public UIShareDocuments(){ }

  public String getValue() {
    return getUIStringInput(USER).getValue();
  }

  public void init() {
    try {
      addChild(UIWhoHasAccess.class, null, null);
      getChild(UIWhoHasAccess.class).init();
      addChild(new UIFormTextAreaInput("textAreaInput", "textAreaInput", ""));
      Node currentNode = this.getNode();
      ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
      ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(SHARECONTENT_BUNDLE_LOCATION, Util.getPortalRequestContext().getLocale());
      String canView = resourceBundle.getString(SHARE_OPTION_CANVEW);
      String canModify = resourceBundle.getString(SHARE_OPTION_CANMODIFY);

      List<SelectItemOption<String>> itemOptions = new ArrayList<SelectItemOption<String>>();

      if(PermissionUtil.canSetProperty(currentNode)) {
        setPermissionDropDown(true);
      }else{
        setPermissionDropDown(false);
      }
      addUIFormInput(new UIFormStringInput(USER, null, null));
      permissions = getAllPermissions();
    } catch (Exception e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
  }


  public String getDocumentName(){
    String[] arr = nodePath.split("/");
    return arr[arr.length - 1];
  }

  public ExtendedNode getNode(){
    ExtendedNode node = (ExtendedNode)NodeLocation.getNodeByLocation(this.node);
    try {
      if (node.isNodeType("exo:symlink") && node.hasProperty("exo:uuid")) {
        LinkManager linkManager = WCMCoreUtils.getService(LinkManager.class);
        return (ExtendedNode)linkManager.getTarget(node);
      }
    } catch (RepositoryException e) {
      LOG.error(e.getMessage(), e);
    }
    return node;
  }

  public String getIconURL(){
    try {
      return Utils.getNodeTypeIcon(getNode(), "uiIcon24x24");
    } catch (RepositoryException e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
    return null;
  }
  public void setSelectedNode(NodeLocation node) {
    this.node = node;
    this.nodePath = node.getPath();
  }

  public Set<String> getWhoHasAccess() {
    Set<String> set = new HashSet<String>();
    try {
      for (AccessControlEntry t : getNode().getACL().getPermissionEntries()) {
        set.add(t.getIdentity());
      }
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return null;
    }
    return set;
  }

  public boolean canEdit(String username) {
    try {
      return getNode().getACL().getPermissions(username).contains("add_node")
          && getNode().getACL().getPermissions(username).contains("set_property");
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return false;
    }
  }

  public String getPermission(String name) {
    return canEdit(name) ? SHARE_PERMISSION_MODIFY : SHARE_PERMISSION_VIEW;
  }

  public boolean isOwner(String username) {
    try {
      return username.equals(getNode().getACL().getOwner());
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      return false;
    }
  }

  public String getOwner() {
    try {
      return getNode().getACL().getOwner();
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }
    return null;
  }

  public Map<String, String> getAllPermissions() {
    Map<String, String> perm = new HashMap<String, String>();
    Iterator it = getWhoHasAccess().iterator();
    while (it.hasNext()) {
      String identity = (String) it.next();
      if (!isOwner(identity)) perm.put(identity, getPermission(identity));
    }
    return perm;
  }

  public String getRestURL() {
    StringBuilder builder = new StringBuilder();
    builder.append("/").append(PortalContainer.getCurrentRestContextName()).append("/social/people/suggest.json?");
    builder.append("currentUser=").append(RequestContext.getCurrentInstance().getRemoteUser());
    builder.append("&typeOfRelation=").append("share_document");
    return builder.toString();
  }

  public String getComment(){
    if(this.comment == null) return "";
    return this.comment;
  }

  private String getDefaultThumbnail(Node node) throws Exception {
    String baseURI = CommonsUtils.getCurrentDomain();
    String cssClass = CssClassUtils.getCSSClassByFileNameAndFileType(
        node.getName() , getMimeType(node), CssClassManager.ICON_SIZE.ICON_64);

    if (cssClass.indexOf(CssClassIconFile.DEFAULT_CSS) > 0) {
      return baseURI + EXO_RESOURCES_URI  + "uiIcon64x64Templatent_file.png";
    }
    return baseURI + EXO_RESOURCES_URI + cssClass.split(" ")[0] + ICON_FILE_EXTENSION;
  }

  private String getMimeType(Node node) throws Exception {
    return DMSMimeTypeResolver.getInstance().getMimeType(node.getName());
  }

  @Override
  public void activate() {  }
  @Override
  public void deActivate() {}


}