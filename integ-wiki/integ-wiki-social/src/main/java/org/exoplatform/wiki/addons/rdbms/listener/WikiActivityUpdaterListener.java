package org.exoplatform.wiki.addons.rdbms.listener;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.wiki.ext.impl.WikiSpaceActivityPublisher;

public class WikiActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(WikiActivityUpdaterListener.class);

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    if (WikiSpaceActivityPublisher.PAGE_ID_KEY.equals(activity.getType())) {
      if (activity.isComment()) {
        LOG.info(String.format("Migration the wiki comment '%s' with old id's %s and new id's %s", activity.getTitle(), activity.getId(), event.getData()));
      } else {
        LOG.info(String.format("Migration the wiki activity '%s' with old id's %s and new id's %s", activity.getTitle(), activity.getId(), event.getData()));
      }
    }
  }
}