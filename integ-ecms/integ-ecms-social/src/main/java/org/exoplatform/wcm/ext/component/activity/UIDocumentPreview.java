package org.exoplatform.wcm.ext.component.activity;


import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.plugin.doc.UIDocViewer;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;
import org.exoplatform.wcm.webui.reader.ContentReader;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.WebuiBindingContext;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.Node;
import java.util.List;
import java.util.Map;

@ComponentConfig(
        template = "war:/groovy/ecm/social-integration/UIDocumentPreview.gtmpl",
        events = {@EventConfig(listeners = {UIDocumentPreview.CloseActionListener.class}, name = "ClosePopup")}
)
public class UIDocumentPreview extends UIPopupWindow {

  private BaseUIActivity baseUIActivity;

  /**
   * Get all comments
   *
   * @return
   */
  private List<ExoSocialActivity> getAllComments() {
    return baseUIActivity.getAllComments();
  }

  private String getCommentMessage(Map<String, String> activityParams) {
    String[] systemComment = Utils.getSystemCommentBundle(activityParams);
    StringBuffer commentBuffer = new StringBuffer();
    if (systemComment != null && systemComment.length > 0) {
      String[] systemCommentTitle = Utils.getSystemCommentTitle(activityParams);
      for (int count = 0; count < systemComment.length; count++) {
        String commentMessage = Utils.getBundleValue(systemComment[count]);
        if (systemCommentTitle != null && systemCommentTitle.length > count) {
          String[] titles = systemCommentTitle[count].split(ActivityCommonService.METADATA_VALUE_SEPERATOR);
          for (int i = 0; i < titles.length; i++) {
            commentMessage = commentMessage.replace("{" + i + "}", titles[i]);
            commentMessage = ContentReader.getXSSCompatibilityContent(commentMessage);
          }
        }

        commentBuffer.append("<p class=\"ContentBlock\">").append(commentMessage).append("</p>");
      }
    }
    return commentBuffer.toString();
  }

  private String[] getSystemCommentTitle(Map<String, String> activityParams) {
    return Utils.getSystemCommentTitle(activityParams);
  }

  private String[] getSystemCommentBundle(Map<String, String> activityParams) {
    return Utils.getSystemCommentBundle(activityParams);
  }

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

  private String getPostedTimeString(WebuiBindingContext resourceBundle, long postedTime) throws Exception {
    return baseUIActivity.getPostedTimeString(resourceBundle, postedTime);
  }

  public void setBaseUIActivity(BaseUIActivity baseUIActivity) {
    this.baseUIActivity = baseUIActivity;
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
