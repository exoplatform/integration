package org.exoplatform.forum.addons.rdbms.listener;

import org.exoplatform.forum.ext.activity.ForumActivityBuilder;
import org.exoplatform.forum.ext.activity.ForumActivityUtils;
import org.exoplatform.forum.service.ForumService;
import org.exoplatform.forum.service.Post;
import org.exoplatform.forum.service.Topic;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;

public class ForumActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(ForumActivityUpdaterListener.class);

  /**
   * Constructor
   * @param forumService 
   * Do not remove forum service on constructor, it use for order Startable of migration.
   */
  public ForumActivityUpdaterListener(ForumService forumService) {
  }

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    String newActivityId = event.getData();
    if (ForumActivityBuilder.FORUM_ACTIVITY_TYPE.equals(activity.getType())) {
      if (activity.isComment()) {
        Post post = ForumActivityUtils.getPost(activity);
        ForumActivityUtils.takeCommentBack(post, newActivityId);
        LOG.info(String.format("Migration the forum comment '%s' with old id's %s and new id's %s", activity.getTitle(), activity.getId(), newActivityId));
      } else {
        Topic topic = ForumActivityUtils.getTopic(activity);
        ForumActivityUtils.takeActivityBack(topic, newActivityId);
        LOG.info(String.format("Migration the forum activity '%s' with old id's %s and new id's %s", activity.getTitle(), activity.getId(), newActivityId));
      }
    }
  }
  
}