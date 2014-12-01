package org.exoplatform.wcm.ext.component.activity;


import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
        template = "war:/groovy/ecm/social-integration/UIDocumentPreview.gtmpl",
        events = {@EventConfig(listeners = {UIDocumentPreview.CloseActionListener.class}, name = "ClosePopup")}
)
public class UIDocumentPreview extends UIPopupWindow {

  public static class CloseActionListener extends EventListener<UIDocumentPreview> {
    public void execute(Event<UIDocumentPreview> event) throws Exception {
      UIDocumentPreview uiDocumentPreview = event.getSource();
      if (!uiDocumentPreview.isShow())
        return;
      uiDocumentPreview.setShow(false);
      uiDocumentPreview.setUIComponent(null);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDocumentPreview.getParent());
    }
  }
}
