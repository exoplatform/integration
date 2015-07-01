package org.exoplatform.wcm.addons.rdbms.listener;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.wcm.ext.component.activity.UILinkActivity;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;

public class WCMActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(WCMActivityUpdaterListener.class);

  public WCMActivityUpdaterListener() {
  }

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity oldActivity = event.getSource();
    switch (oldActivity.getType()) {
    case UILinkActivity.ACTIVITY_TYPE:
      migrationLinkActivity(oldActivity, event.getData());
      break;
    case Utils.CONTENT_SPACES:
      migrationContentSpaceActivity(oldActivity, event.getData());
      break;
    case Utils.FILE_SPACES:
      migrationFileSpaceActivity(oldActivity, event.getData());
      break;
    default:
      break;
    }
  }

  private void migrationLinkActivity(ExoSocialActivity oldActivity, String newId) {
    if (oldActivity.isComment()) {
      LOG.info(String.format("Migration the %s link comment '%s' with old id's %s and new id's %s", oldActivity.getType(), oldActivity.getTitle(), oldActivity.getId(), newId));
    } else {
      LOG.info(String.format("Migration the %s link activity '%s' with old id's %s and new id's %s", oldActivity.getType(), oldActivity.getTitle(), oldActivity.getId(), newId));
    }
  }

  private void migrationContentSpaceActivity(ExoSocialActivity oldActivity, String newId) {
    if (oldActivity.isComment()) {
      LOG.info(String.format("Migration the %s ContentSpace comment '%s' with old id's %s and new id's %s", oldActivity.getType(), oldActivity.getTitle(), oldActivity.getId(), newId));
    } else {
      LOG.info(String.format("Migration the %s ContentSpace activity '%s' with old id's %s and new id's %s", oldActivity.getType(), oldActivity.getTitle(), oldActivity.getId(), newId));
    }
  }

  private void migrationFileSpaceActivity(ExoSocialActivity oldActivity, String newId) {
    if (oldActivity.isComment()) {
      LOG.info(String.format("Migration the %s FileSpace comment '%s' with old id's %s and new id's %s", oldActivity.getType(), oldActivity.getTitle(), oldActivity.getId(), newId));
    } else {
      LOG.info(String.format("Migration the %s FileSpace activity '%s' with old id's %s and new id's %s", oldActivity.getType(), oldActivity.getTitle(), oldActivity.getId(), newId));
    }
  }

}