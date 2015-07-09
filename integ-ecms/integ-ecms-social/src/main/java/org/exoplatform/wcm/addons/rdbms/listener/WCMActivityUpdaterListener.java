package org.exoplatform.wcm.addons.rdbms.listener;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.exoplatform.commons.utils.ActivityTypeUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
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
    String workspace = activity.getTemplateParams().get(UIDocActivity.WORKSPACE);
    if (workspace == null) {
      workspace = activity.getTemplateParams().get(UIDocActivity.WORKSPACE.toLowerCase());
    }
    String docId = activity.getTemplateParams().get(UIDocActivity.ID);
    String path = activity.getUrl();
    LOG.info("Migration doc: " + workspace + " " + docId + " " + path);
    Node docNode = getDocNode(workspace, path, docId);
    if (docNode != null && docNode.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
      LOG.info("Migration doc: " + docNode.getPath());
      try {
        ActivityTypeUtils.attachActivityId(docNode, newId);
        docNode.save();
      } catch (RepositoryException e) {
        LOG.warn("Updates the file-spaces activity is unsuccessful!");
        LOG.debug("Updates the file-spaces activity is unsuccessful!", e);
      }
    }
  }

  /**
   * This method is target to get the Document node.
   * 
   * @param workspace
   * @param path
   * @param nodeId
   * @return
   */
  private Node getDocNode(String workspace, String path, String nodeId) {
    NodeLocation nodeLocation = new NodeLocation();
    nodeLocation.setSystemSession(true);
    nodeLocation.setUUID(nodeId);
    nodeLocation.setPath(path);
    //
    return NodeLocation.getNodeByLocation(nodeLocation);
  }
}