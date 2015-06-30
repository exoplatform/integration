package org.exoplatform.social.addons.rdbms.listener;

import static org.exoplatform.social.plugin.doc.UIDocActivityBuilder.ACTIVITY_TYPE;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public class DocActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(DocActivityUpdaterListener.class);

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    if (ACTIVITY_TYPE.equals(activity.getType())) {
      if (activity.isComment()) {
        LOG.info(String.format("Migration %s doc comment '%s' with old id's %s and new id's %s", activity.getType(), activity.getTitle(), activity.getId(), event.getData()));
      } else {
        LOG.info(String.format("Migration %s doc activity '%s' with old id's %s and new id's %s", activity.getType(), activity.getTitle(), activity.getId(), event.getData()));
      }
    }
  }
}