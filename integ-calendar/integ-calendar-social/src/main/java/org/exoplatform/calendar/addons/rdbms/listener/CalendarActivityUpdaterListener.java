package org.exoplatform.calendar.addons.rdbms.listener;

import org.exoplatform.cs.ext.impl.CalendarSpaceActivityPublisher;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public class CalendarActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(CalendarActivityUpdaterListener.class);

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    if (CalendarSpaceActivityPublisher.CALENDAR_APP_ID.equals(activity.getType())) {
      LOG.info(String.format("Migration the calendar activity '%s' with old id's %s and new id's %s", activity.getTitle(), activity.getId(), event.getData()));
    } else if (CalendarSpaceActivityPublisher.ACTIVITY_COMMENT_TYPE.equals(activity.getType()) && activity.isComment()) {
      LOG.info(String.format("Migration the calendar comment '%s' with old id's %s and new id's %s", activity.getTitle(), activity.getId(), event.getData()));
    }
  }
}