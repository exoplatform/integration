package org.exoplatform.wcm.ext.component.activity;


import org.exoplatform.social.plugin.doc.UIDocViewer;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.Node;

@ComponentConfig(
        template = "war:/groovy/ecm/social-integration/UIDocumentPreview.gtmpl",
        events = {@EventConfig(listeners = {UIDocumentPreview.CloseActionListener.class}, name = "ClosePopup")}
)
public class UIDocumentPreview extends UIPopupWindow {

  private boolean isWebContent() throws Exception {
    UIDocViewer uiDocViewer = findFirstComponentOfType(UIDocViewer.class);
    Node previewNode = uiDocViewer.getNode();
    if (previewNode != null) {
      return previewNode.isNodeType(org.exoplatform.ecm.webui.utils.Utils.EXO_WEBCONTENT);
    }

    return false;
  }

  private Node getOriginalNode() throws Exception {
    UIDocViewer uiDocViewer = findFirstComponentOfType(UIDocViewer.class);
    return uiDocViewer.getOriginalNode();
   }

  public static class CloseActionListener extends EventListener<UIDocumentPreview> {
    public void execute(Event<UIDocumentPreview> event) throws Exception {
      UIDocumentPreview uiDocumentPreview = event.getSource();
      if (!uiDocumentPreview.isShow())
        return;
      uiDocumentPreview.setShow(false);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentPreview.getParent());
    }
  }
}
