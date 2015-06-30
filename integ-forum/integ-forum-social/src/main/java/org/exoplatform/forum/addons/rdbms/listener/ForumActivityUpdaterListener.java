package org.exoplatform.forum.addons.rdbms.listener;

import org.exoplatform.forum.ext.activity.ForumActivityBuilder;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public class ForumActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(ForumActivityUpdaterListener.class);

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    if (ForumActivityBuilder.FORUM_ACTIVITY_TYPE.equals(activity.getType())) {
      if (activity.isComment()) {
        LOG.info(String.format("Migration the forum comment '%s' with old id's %s and new id's %s", activity.getTitle(), activity.getId(), event.getData()));
      } else {
        LOG.info(String.format("Migration the forum activity '%s' with old id's %s and new id's %s", activity.getTitle(), activity.getId(), event.getData()));
      }
    }
  }
}