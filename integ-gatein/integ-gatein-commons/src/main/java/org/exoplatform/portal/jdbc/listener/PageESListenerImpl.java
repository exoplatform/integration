package org.exoplatform.portal.jdbc.listener;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.jdbc.service.PageIndexingServiceConnector;
import org.exoplatform.portal.mop.EventType;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class PageESListenerImpl extends Listener<PageService, PageKey> {

  private static final Log LOG = ExoLogger.getExoLogger(PageESListenerImpl.class);

  @Override
  public void onEvent(Event<PageService, PageKey> event) throws Exception {
    String eventName = event.getEventName();
    PageKey pageKey = event.getData();
    if (EventType.PAGE_CREATED.equals(eventName)) {
      pageCreated(pageKey);
    } else if (EventType.PAGE_UPDATED.equals(eventName)) {
      pageUpdated(pageKey);
    } else if (EventType.PAGE_DESTROYED.equals(eventName)) {
      pageDestroyed(pageKey);
    }
  }

  private void pageDestroyed(PageKey pageKey) {
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);

    LOG.debug("Notifying indexing service for page removal id={}", pageKey.format());

    indexingService.unindex(PageIndexingServiceConnector.TYPE, pageKey.format());
  }

  private void pageUpdated(PageKey pageKey) {
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);

    LOG.debug("Notifying indexing service for reindexing page removal id={}", pageKey.format());

    indexingService.reindex(PageIndexingServiceConnector.TYPE, pageKey.format());
  }

  private void pageCreated(PageKey pageKey) {
    IndexingService indexingService = CommonsUtils.getService(IndexingService.class);

    LOG.info("Notifying indexing service for page creation pageKey={}", pageKey.format());

    indexingService.index(PageIndexingServiceConnector.TYPE, pageKey.format());
  }

}
