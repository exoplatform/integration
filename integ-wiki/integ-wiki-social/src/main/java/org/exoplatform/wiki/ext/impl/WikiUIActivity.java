package org.exoplatform.wiki.ext.impl;

import java.util.Map;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.SpaceStorageException;
import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.lifecycle.WebuiBindingContext;

@ComponentConfig (
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/wiki/social-integration/plugin/space/WikiUIActivity.gtmpl",
    events = {
        @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
        @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
        @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class, confirm = "UIActivity.msg.Are_You_Sure_To_Delete_This_Activity"),
        @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class, confirm = "UIActivity.msg.Are_You_Sure_To_Delete_This_Comment") 
      }
)
public class WikiUIActivity extends BaseUIActivity {
  private static final Log LOG = ExoLogger.getLogger(WikiUIActivity.class);

  public WikiUIActivity() {
  }

  public String getUriOfAuthor() {   
    if (getOwnerIdentity() == null){
      if (LOG.isDebugEnabled()) {
        LOG.debug("Failed to get Url of user, author isn't set");        
      }       
      return "";
    }        
    return new StringBuilder().append("<a href='").append(getOwnerIdentity().getProfile().getUrl()).append("'>")
                                .append(getOwnerIdentity().getProfile().getFullName()).append("</a>").toString();    
  }

  public String getUserFullName(String userId) {
    return getOwnerIdentity().getProfile().getFullName();
  }

  public String getUserProfileUri(String userId) {
    return getOwnerIdentity().getProfile().getUrl();
  }

  public String getUserAvatarImageSource(String userId) {
    return getOwnerIdentity().getProfile().getAvatarUrl();
  }

  public String getSpaceAvatarImageSource(String spaceIdentityId) {    
    try {
      if (getOwnerIdentity() == null){
        LOG.error("Failed to get Space Avatar Source, unknow owner identity.");
        return null;
      }
      String spaceId = getOwnerIdentity().getRemoteId();
      SpaceService spaceService = getApplicationComponent(SpaceService.class);
      Space space = spaceService.getSpaceById(spaceId);
      if (space != null) {
        return space.getAvatarUrl();
      }
    } catch (SpaceStorageException e) { // SpaceService
      LOG.error(String.format("Failed to getSpaceById: %s. \n Cause by: ", spaceIdentityId), e);
    }

    return null;
  }
  
  public String getActivityParamValue(String key) {
    String value = null;
    Map<String, String> params = getActivity().getTemplateParams();
    if (params != null) {
      value = params.get(key);
    }
    return value != null ? value : "";
  }

  String getActivityMessage(WebuiBindingContext _ctx) throws Exception {
    String activityType = getActivityParamValue(WikiSpaceActivityPublisher.ACTIVITY_TYPE_KEY);
    if (activityType.equalsIgnoreCase(WikiSpaceActivityPublisher.ADD_PAGE_TYPE)) {
      return _ctx.appRes("WikiUIActivity.label.page-create");
    } else if (WikiSpaceActivityPublisher.UPDATE_PAGE_TYPE.equalsIgnoreCase(activityType)) {
      return _ctx.appRes("WikiUIActivity.label.page-update");
    }
    return "";
  }

  String getPageName() {
    return getActivityParamValue(WikiSpaceActivityPublisher.PAGE_TITLE_KEY);
  }

  String getPageURL() {
    return getActivityParamValue(WikiSpaceActivityPublisher.URL_KEY);
  }
  
  String getViewChangeURL(){
    return getActivityParamValue(WikiSpaceActivityPublisher.VIEW_CHANGE_URL_KEY);
  }
  
  String getPageExcerpt(){
    return getActivityParamValue(WikiSpaceActivityPublisher.PAGE_EXCERPT);
  }

}
