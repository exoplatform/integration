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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.ecm.utils.permission.PermissionUtil;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.commons.EventUIComponent;
import org.exoplatform.webui.commons.EventUIComponent.EVENTTYPE;
import org.exoplatform.webui.commons.UISpacesSwitcher;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupContainer;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.wcm.ext.component.document.service.IShareDocumentService;
import org.exoplatform.ecm.webui.utils.Utils;
import org.exoplatform.portal.webui.util.Util;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 18, 2014 
 */
@ComponentConfig(
                 lifecycle = UIFormLifecycle.class,
                 template =  "classpath:groovy/ecm/social-integration/UIShareDocuments.gtmpl",
                 events = {
                   @EventConfig(listeners = UIShareDocuments.ShareActionListener.class),
                   @EventConfig(listeners = UIShareDocuments.CancelActionListener.class),
                   @EventConfig(listeners = UIShareDocuments.TextChangeActionListener.class),
                   @EventConfig(listeners = UIShareDocuments.RemoveSpaceActionListener.class),
                   @EventConfig(listeners = UIShareDocuments.SelectSpaceActionListener.class, phase=Phase.PROCESS)
                 }
    )
public class UIShareDocuments extends UIForm implements UIPopupComponent{

  private static final Log    LOG                 = ExoLogger.getLogger(UIShareDocuments.class);
  private static final String SHARECONTENT_BUNDLE_LOCATION = "locale.ShareDocuments";
  private static final String SHARE_OPTION_CANVEW          = "UIShareDocuments.label.option.read";
  private static final String SHARE_OPTION_CANMODIFY       = "UIShareDocuments.label.option.modify";

  private static final String SHARE_PERMISSION_VIEW        = PermissionType.READ;
  private static final String SHARE_PERMISSION_MODIFY      = "modify";

  public static class RemoveSpaceActionListener extends EventListener<UIShareDocuments>{

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      UIShareDocuments uiform = event.getSource();
      uiform.spaces.remove(event.getRequestContext().getRequestParameter(OBJECTID).toString());
      if (event.getSource().getChild(UIFormTextAreaInput.class).getValue() == null) uiform.comment = "";
      else uiform.comment = event.getSource().getChild(UIFormTextAreaInput.class).getValue();
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer objUIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(objUIPopupContainer);
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

  public static class SelectSpaceActionListener extends EventListener<UIShareDocuments>{

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      List<String> spaces = event.getSource().spaces;
      event.getSource().comment = event.getSource().getChild(UIFormTextAreaInput.class).getValue();
      String space = event.getRequestContext().getRequestParameter(UISpacesSwitcher.SPACE_ID_PARAMETER).toString();
      //space = space.split("/")[2];
      if(!spaces.contains(space))
        spaces.add(space);
      UIJCRExplorer uiExplorer = event.getSource().getAncestorOfType(UIJCRExplorer.class);
      UIPopupContainer objUIPopupContainer = uiExplorer.getChild(UIPopupContainer.class);
      event.getRequestContext().addUIComponentToUpdateByAjax(objUIPopupContainer);
    }
  }

  public static class ShareActionListener extends EventListener<UIShareDocuments>{

    @Override
    public void execute(Event<UIShareDocuments> event) throws Exception {
      if(event.getSource().spaces.size() > 0){
        IShareDocumentService service = WCMCoreUtils.getService(IShareDocumentService.class);
        //ShareDocumentService service = new ShareDocumentService(WCMCoreUtils.getService(RepositoryService.class), WCMCoreUtils.getService(LinkManager.class), WCMCoreUtils.getService(SessionProviderService.class));
        List<String> spaces = event.getSource().spaces;
        Node node = event.getSource().getNode();
        String message = "";
        String perm = "modify";
        if(event.getSource().getChild(UIFormTextAreaInput.class).getValue() != null) message = event.getSource().getChild(UIFormTextAreaInput.class).getValue();
        if(event.getSource().getChild(UIFormSelectBox.class).getValue() != null) perm = event.getSource().getChild(UIFormSelectBox.class).getValue();
        for(String space : spaces){
          if(space.equals("")) continue;
          else service.publicDocumentToSpace(space,node,message,perm);
        }
        event.getSource().getAncestorOfType(UIJCRExplorer.class).cancelAction() ;
      }else{
        UIShareDocuments uicomp = event.getSource() ;
        UIApplication uiApp = uicomp.getAncestorOfType(UIApplication.class);
        uiApp.addMessage(new ApplicationMessage("UIShareDocuments.label.NoSpace", null,
                                                ApplicationMessage.WARNING)) ;
      }
    }
  }

  private String nodePath;
  List<String> spaces = new ArrayList<String>();
  public String comment = "";
  private NodeLocation node;

  public UIShareDocuments(){ }

  public void init(){
    try {
      addChild(UISpacesSwitcher.class, null, "SpaceSwitcher");
      getChild(UISpacesSwitcher.class).setShowPortalSpace(false);
      getChild(UISpacesSwitcher.class).setShowUserSpace(false);

      EventUIComponent temp = new EventUIComponent("UIShareDocuments","SelectSpace",EVENTTYPE.EVENT);
      getSpace().init(temp);
      ArrayList<SelectItemOption<String>> permOption = new ArrayList<SelectItemOption<String>>();

      addChild(new UIFormSelectBox("permissionDropDown", "permissionDropDown", permOption));

      ResourceBundleService resourceBundleService = WCMCoreUtils.getService(ResourceBundleService.class);
      ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(SHARECONTENT_BUNDLE_LOCATION, Util.getPortalRequestContext().getLocale());
      String canView = resourceBundle.getString(SHARE_OPTION_CANVEW);
      String canModify = resourceBundle.getString(SHARE_OPTION_CANMODIFY);

      List<SelectItemOption<String>> itemOptions = new ArrayList<SelectItemOption<String>>();

      Node currentNode = this.getNode();
      if(PermissionUtil.canSetProperty(currentNode)) {
        itemOptions.add(new SelectItemOption<String>(canView, SHARE_PERMISSION_VIEW));
        itemOptions.add(new SelectItemOption<String>(canModify, SHARE_PERMISSION_MODIFY));
      }else if(PermissionUtil.canRead(currentNode)){
        itemOptions.add(new SelectItemOption<String>(canView, SHARE_PERMISSION_VIEW));
      }

      getChild(UIFormSelectBox.class).setOptions(itemOptions);
      addChild(new UIFormTextAreaInput("textAreaInput", "textAreaInput", ""));
    } catch (Exception e) {
      if(LOG.isErrorEnabled())
        LOG.error(e.getMessage(), e);
    }
  }

  public String getDocumentName(){
    String[] arr = nodePath.split("/");
    return arr[arr.length - 1];
  }

  public Node getNode(){
    return NodeLocation.getNodeByLocation(this.node);
  }

  public String getFileExtension(){
    int index = nodePath.lastIndexOf('.');
    if (index != -1) {
      return nodePath.substring(index);
    } else {
      return "";
    }
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

  public UISpacesSwitcher getSpace(){
    return getChild(UISpacesSwitcher.class);
  }
  public List<String> getSpaces(){
    List<String> lstSpaceDisplayName = new ArrayList<String>();
    SpaceService spaceService= PortalContainer.getInstance().getComponentInstanceOfType(SpaceService.class);

    for(String space:spaces){
      Space spaceObject = spaceService.getSpaceByGroupId(space);
      if(space!=null){
        lstSpaceDisplayName.add(spaceObject.getDisplayName());
      }
    }
    return lstSpaceDisplayName;
  }
  public String getComment(){
    if(this.comment == null) return "";
    return this.comment;
  }
  @Override
  public void activate() {  }
  @Override
  public void deActivate() {}


}
