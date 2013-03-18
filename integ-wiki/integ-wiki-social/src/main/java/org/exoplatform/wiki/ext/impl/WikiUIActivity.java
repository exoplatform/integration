package org.exoplatform.wiki.ext.impl;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.webui.util.Util;
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
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.resolver.PageResolver;
import org.exoplatform.wiki.service.WikiService;

@ComponentConfig (
    lifecycle = UIFormLifecycle.class,
    template = "classpath:groovy/wiki/social-integration/plugin/space/WikiUIActivity.gtmpl",
    events = {
        @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
        @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
        @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
        @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
        @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class) 
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
    return _ctx.appRes("WikiUIActivity.label.page-create");
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
  
  String getPageVersion(){
    String version = getActivityParamValue(WikiSpaceActivityPublisher.WIKI_PAGE_VERSION);
    if (StringUtils.isEmpty(version)) {
      version = "1";
      String pageUrl = getPageURL();
      if (pageUrl == null) {
        return version;
      }
      
      PageResolver pageResolver = (PageResolver) PortalContainer.getComponent(PageResolver.class);
      WikiService wikiService = (WikiService) PortalContainer.getInstance().getComponentInstanceOfType(WikiService.class);
      try {
        Page wikiHome = pageResolver.resolve(pageUrl, Util.getUIPortal().getSelectedUserNode());
        if (wikiHome != null) {
          Page page = wikiService.getPageById(wikiHome.getWiki().getType(), wikiHome.getWiki().getOwner(), pageUrl.substring(pageUrl.lastIndexOf('/') + 1));
          if (page != null) {
            version = String.valueOf(page.getVersionableMixin().getVersionHistory().getChildren().size() - 1);
          }
        }
      } catch (Exception e) {
        LOG.warn("Failed to get version of wiki page", e);
      }
    }
    return version;
  }
}
