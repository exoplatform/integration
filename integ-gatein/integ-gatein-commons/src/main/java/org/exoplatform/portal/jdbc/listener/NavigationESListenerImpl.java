package org.exoplatform.portal.jdbc.listener;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.jdbc.service.NavigationIndexingServiceConnector;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.portal.mop.user.UserNavigation;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NavigationESListenerImpl extends Listener<NavigationService, SiteKey> {

  private static final Log LOG = ExoLogger.getExoLogger(NavigationESListenerImpl.class);

  private IndexingService indexingService;

  private NavigationService navigationService;

  public NavigationESListenerImpl(IndexingService indexingService, NavigationService navigationService) {
    this.indexingService = indexingService;
    this.navigationService = navigationService;
  }

  @Override
  public void onEvent(Event<NavigationService, SiteKey> event) throws Exception {
    SiteKey siteKey = event.getData();
    LOG.info("Notifying indexing service for navigation={}", siteKey);

    if (EventType.NAVIGATION_DESTROY.equals(event.getEventName())) {
      NavigationContext nav = navigationService.loadNavigation(siteKey);
      NodeContext node = navigationService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
      unIndexTree(node);
    } else if (EventType.NAVIGATION_CREATED.equals(event.getEventName())) {
      NavigationContext nav = navigationService.loadNavigation(siteKey);
      NodeContext node = navigationService.loadNode(NodeModel.SELF_MODEL, nav, Scope.ALL, null);
      indexTree(node);
    }
  }

  private void unIndexTree(NodeContext node) {
    indexingService.unindex(NavigationIndexingServiceConnector.TYPE, node.getId());
    if (node.getNodes() != null) {
      for (Object child : node.getNodes()) {
        unIndexTree((NodeContext) child);
      }
    }
  }

  private void indexTree(NodeContext node) {
    indexingService.index(NavigationIndexingServiceConnector.TYPE, node.getId());
    if (node.getNodes() != null) {
      for (Object child : node.getNodes()) {
        indexTree((NodeContext) child);
      }
    }
  }
}
