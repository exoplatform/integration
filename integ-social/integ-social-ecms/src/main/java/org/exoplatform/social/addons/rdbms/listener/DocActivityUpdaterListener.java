package org.exoplatform.social.addons.rdbms.listener;

import static org.exoplatform.social.plugin.doc.UIDocActivityBuilder.ACTIVITY_TYPE;

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

public class DocActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(DocActivityUpdaterListener.class);

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    if (ACTIVITY_TYPE.equals(activity.getType())) {
      String workspace = activity.getTemplateParams().get(UIDocActivity.WORKSPACE);
      String docId = activity.getTemplateParams().get(UIDocActivity.ID);
      String path = activity.getUrl();

      Node docNode = getDocNode(workspace, path, docId);
      if (docNode != null && docNode.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
        try {
          ActivityTypeUtils.attachActivityId(docNode, event.getData());
          docNode.save();
        } catch (RepositoryException e) {
          LOG.warn("Updates the document activity is unsuccessful!");
          LOG.debug("Updates the document activity is unsuccessful!", e);
        }
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