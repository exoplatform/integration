package org.exoplatform.wcm.addons.rdbms.listener;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.plugin.doc.UIDocActivity;
import org.exoplatform.wcm.ext.component.activity.UILinkActivity;
import org.exoplatform.wcm.ext.component.activity.listener.Utils;

public class WCMActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(WCMActivityUpdaterListener.class);

  public WCMActivityUpdaterListener() {
  }

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity oldActivity = event.getSource();
    String type = (oldActivity.getType() == null) ? "" : oldActivity.getType();
    switch (type) {
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
  }

  private void migrationContentSpaceActivity(ExoSocialActivity oldActivity, String newId) {
  }

  private void migrationFileSpaceActivity(ExoSocialActivity activity, String newId) throws RepositoryException {
    if (activity.isComment()) {
      // TODO: Needs to confirm with ECMS team about the comment type
      // Utils.CONTENT_SPACES = "contents:spaces" Asks ECMS team to update the comment
      // There is new mixin type define to keep the CommentId
      // private static String MIX_COMMENT = "exo:activityComment";
      // private static String MIX_COMMENT_ID = "exo:activityCommentID";
      LOG.info(String.format("Migration file-spaces comment '%s' with new id's %s", activity.getTitle(), newId));
      //
      migrationDoc(activity, newId);
    } else {
      LOG.info(String.format("Migration file-spaces activity '%s' with new id's %s", activity.getTitle(), newId));
      //
      migrationDoc(activity, newId);
    }
  }

  private void migrationDoc(ExoSocialActivity activity, String newId) throws RepositoryException {
    String docId = activity.getTemplateParams().get(UIDocActivity.ID);
    Node docNode = getDocNode(docId);
    ActivityTypeUtils.attachActivityId(docNode, newId);
    docNode.getSession().save();
  }

  /**
   * This method is target to get the Document node.
   * 
   * @param nodeId
   * @return
   */
  private Node getDocNode(String nodeId) {
    ManageableRepository manageRepo = CommonsUtils.getRepository();
    SessionProvider sessionProvider = SessionProvider.createSystemProvider();
    Node contentNode = null;
    for (String ws : manageRepo.getWorkspaceNames()) {
      try {
        contentNode = sessionProvider.getSession(ws, manageRepo).getNodeByUUID(nodeId);
        break;
      } catch (RepositoryException e) {
        continue;
      }
    }
    //
    return contentNode;
  }
}