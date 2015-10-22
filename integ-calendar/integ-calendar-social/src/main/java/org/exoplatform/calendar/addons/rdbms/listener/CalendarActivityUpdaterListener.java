package org.exoplatform.calendar.addons.rdbms.listener;

import javax.jcr.Node;

import org.exoplatform.calendar.service.CalendarService;
import org.exoplatform.calendar.service.impl.JCRDataStorage;
import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.cs.ext.impl.CalendarSpaceActivityPublisher;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public class CalendarActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log       LOG = ExoLogger.getLogger(CalendarActivityUpdaterListener.class);
  private JCRDataStorage storage_;

  public CalendarActivityUpdaterListener(CalendarService calendarService) {
  }

  private JCRDataStorage getJCRDataStorage() throws Exception {
    if (this.storage_ == null) {
      this.storage_ = new JCRDataStorage(CommonsUtils.getService(NodeHierarchyCreator.class),
                                         CommonsUtils.getService(RepositoryService.class),
                                         CommonsUtils.getService(CacheService.class));
    }
    return this.storage_;
  }

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    if (CalendarSpaceActivityPublisher.CALENDAR_APP_ID.equals(activity.getType())) {
      String eventId = activity.getTemplateParams().get(CalendarSpaceActivityPublisher.EVENT_ID_KEY);
      String calendarId = activity.getTemplateParams().get(CalendarSpaceActivityPublisher.CALENDAR_ID_KEY);
      //
      Node calendarNode = getJCRDataStorage().getPublicCalendarHome().getNode(calendarId);
      Node eventNode = calendarNode.getNode(eventId);
      ActivityTypeUtils.attachActivityId(eventNode, event.getData());
      //
      eventNode.getSession().save();
      LOG.info(String.format("Done migration the calendar activity with old id's %s and new id's %s", activity.getId(), event.getData()));
    }
  }
}