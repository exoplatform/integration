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
import org.exoplatform.social.plugin.doc.UIDocActivityComposer;

public class DocActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(DocActivityUpdaterListener.class);

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    if (ACTIVITY_TYPE.equals(activity.getType())) {
      String newId = event.getData();
      
      if (activity.isComment()) {
        LOG.info(String.format("Migration doc comment '%s' with new id's %s", activity.getTitle(), newId));
        String docCommentId = activity.getTemplateParams().get(UIDocActivity.ID);
        Node docCommentNode = getDocNode(UIDocActivityComposer.REPOSITORY, UIDocActivityComposer.WORKSPACE, docCommentId);
        //TODO: Needs to confirm with ECMS team about the comment type Utils.CONTENT_SPACES = "contents:spaces"
        // Asks ECMS team to update the comment 
        // There is new mixin type define to keep the CommentId
        //private static String MIX_COMMENT                = "exo:activityComment";
        //private static String MIX_COMMENT_ID             = "exo:activityCommentID";
        if (docCommentNode.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
          try {
            docCommentNode.setProperty(ActivityTypeUtils.EXO_ACTIVITY_ID, newId);
            docCommentNode.save();
          } catch (RepositoryException e) {
            LOG.warn("Updates the document activity is unsuccessful!");
            LOG.debug("Updates the document activity is unsuccessful!", e);
          }
        }
        
      } else {
        LOG.info(String.format("Migration doc activity '%s' with new id's %s", activity.getTitle(), newId));
        
        String docId = activity.getTemplateParams().get(UIDocActivity.ID);
        if (docId == null) return;
        
        Node docNode = getDocNode(UIDocActivityComposer.REPOSITORY, UIDocActivityComposer.WORKSPACE, docId);
        if (docNode.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)) {
          try {
            docNode.setProperty(ActivityTypeUtils.EXO_ACTIVITY_ID, newId);
            docNode.save();
          } catch (RepositoryException e) {
            LOG.warn("Updates the document activity is unsuccessful!");
            LOG.debug("Updates the document activity is unsuccessful!", e);
          }
        }
      }
    }
  }
  
  /**
   * This method is target to get the Document node following 
   * the way in UIDocActivityComposer.getDocNode().
   * 
   * @param repository
   * @param workspace
   * @param nodeId
   * @return
   */
  private Node getDocNode(String repository, String workspace, String nodeId) {
    NodeLocation nodeLocation = new NodeLocation(repository, workspace, null, nodeId);
    return NodeLocation.getNodeByLocation(nodeLocation);
  }
}