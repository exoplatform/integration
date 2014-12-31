package org.exoplatform.wcm.ext.component.activity;

import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIApplication;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.core.lifecycle.WebuiBindingContext;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

import javax.jcr.Node;
import java.util.List;
import java.util.Map;

@ComponentConfig(
        template = "war:/groovy/ecm/social-integration/UIPreviewCommentArea.gtmpl",
        events = {
                @EventConfig(listeners = UIPreviewCommentArea.RefreshCommentsActionListener.class),
                @EventConfig(listeners = UIPreviewCommentArea.RemoveCommentActionListener.class,
                        confirm = "UIActivity.msg.Are_You_Sure_To_Delete_This_Comment")
        }
)
public class UIPreviewCommentArea extends UIComponent {

  public static final String REFRESH_COMMENTS = "RefreshComments";
  public static final String REMOVE_COMMENT = "RemoveComment";

  private Node getOriginalNode() throws Exception {
    UIDocumentPreview uiDocumentPreview = this.getParent();
    return uiDocumentPreview.getOriginalNode();
  }

  private String getActivityId() {
    UIDocumentPreview uiDocumentPreview = this.getParent();
    return uiDocumentPreview.getBaseUIActivity().getActivity().getId();
  }

  private boolean isCommentDeletable(String activityUserId) throws SpaceException {
    UIDocumentPreview uiDocumentPreview = this.getParent();
    return uiDocumentPreview.getBaseUIActivity().isCommentDeletable(activityUserId);
  }

  private List<ExoSocialActivity> getAllComments() {
    UIDocumentPreview uiDocumentPreview = this.getParent();
    return uiDocumentPreview.getBaseUIActivity().getAllComments();
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
            commentMessage = org.exoplatform.wcm.ext.component.activity.listener.Utils.getFirstSummaryLines(commentMessage);
          }
        }

        commentBuffer.append("<p class=\"ContentBlock\">").append(commentMessage).append("</p>");
      }
    }
    return commentBuffer.toString();
  }

  private String getPostedTimeString(WebuiBindingContext resourceBundle, long postedTime) throws Exception {
    UIDocumentPreview uiDocumentPreview = this.getParent();
    return uiDocumentPreview.getBaseUIActivity().getPostedTimeString(resourceBundle, postedTime);
  }

  private String[] getSystemCommentTitle(Map<String, String> activityParams) {
    return Utils.getSystemCommentTitle(activityParams);
  }

  private String[] getSystemCommentBundle(Map<String, String> activityParams) {
    return Utils.getSystemCommentBundle(activityParams);
  }

  private boolean isNoLongerExisting(String activityId, Event<UIPreviewCommentArea> event) {
    ExoSocialActivity existingActivity = org.exoplatform.social.webui.Utils.getActivityManager().getActivity
            (activityId);
    if (existingActivity == null) {
      UIApplication uiApplication = event.getRequestContext().getUIApplication();
      uiApplication.addMessage(new ApplicationMessage("BaseUIActivity.msg.info.Activity_No_Longer_Exist",
              null,
              ApplicationMessage.INFO));
      return true;
    }
    return false;
  }

  public static class RefreshCommentsActionListener extends EventListener<UIPreviewCommentArea> {
    public void execute(Event<UIPreviewCommentArea> event) throws Exception {
      UIPreviewCommentArea uiPreviewCommentArea = event.getSource();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiPreviewCommentArea);
    }
  }

  public static class RemoveCommentActionListener extends EventListener<UIPreviewCommentArea> {
    public void execute(Event<UIPreviewCommentArea> event) throws Exception {
      UIPreviewCommentArea uiPreviewCommentArea = event.getSource();
      String activityId = uiPreviewCommentArea.getActivityId();
      String commentId = event.getRequestContext().getRequestParameter(OBJECTID);

      if (uiPreviewCommentArea.isNoLongerExisting(activityId, event) ||
              uiPreviewCommentArea.isNoLongerExisting(commentId, event)) {
        return;
      }

      org.exoplatform.social.webui.Utils.getActivityManager().deleteComment(activityId, commentId);

      event.getRequestContext().addUIComponentToUpdateByAjax(uiPreviewCommentArea);
    }
  }
}
