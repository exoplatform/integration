package org.exoplatform.news;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.news.model.News;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.Utils;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionManager;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionMode;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

/**
 * Service managing News and storing them in ECMS
 */
public class NewsServiceImpl implements NewsService {

  public static final String NEWS_NODES_FOLDER = "News";

  private RepositoryService repositoryService;

  private SessionProviderService sessionProviderService;

  private NodeHierarchyCreator nodeHierarchyCreator;

  private DataDistributionType dataDistributionType;

  private SpaceService spaceService;

  private ActivityManager activityManager;

  private IdentityManager identityManager;

  private UploadService uploadService;

  public NewsServiceImpl(RepositoryService repositoryService, SessionProviderService sessionProviderService,
                         NodeHierarchyCreator nodeHierarchyCreator, DataDistributionManager dataDistributionManager,
                         SpaceService spaceService, ActivityManager activityManager, IdentityManager identityManager,
                         UploadService uploadService) {
    this.repositoryService = repositoryService;
    this.sessionProviderService = sessionProviderService;
    this.nodeHierarchyCreator = nodeHierarchyCreator;
    this.spaceService = spaceService;
    this.activityManager = activityManager;
    this.identityManager = identityManager;
    this.uploadService = uploadService;
    this.dataDistributionType = dataDistributionManager.getDataDistributionType(DataDistributionMode.NONE);
  }

  /**
   * Create a News
   * A news is composed of an activity and a CMS node containing the data
   * @param news The news to create
   * @throws RepositoryException
   */
  public News createNews(News news) throws Exception {
    String newsId = createNewsNode(news);

    news.setId(newsId);

    postNewsActivity(news);

    return news;
  }

  /**
   * Get a news by id
   * @param id Id of the news
   * @return The news with the given id
   * @throws Exception
   */
  public News getNews(String id) throws Exception {
    SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);

    Session session = sessionProvider.getSession(repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName(),
            repositoryService.getCurrentRepository());

    try {
      Node node = session.getNodeByUUID(id);
      return convertNodeToNews(node);
    } catch (ItemNotFoundException e) {
      return null;
    } finally {
      if(session != null) {
        session.logout();
      }
    }
  }

  /**
   * Create the exo:news node in CMS
   * @param news
   * @return
   * @throws Exception
   */
  private String createNewsNode(News news) throws Exception {
    SessionProvider sessionProvider = sessionProviderService.getSystemSessionProvider(null);
    Session session = sessionProvider.getSession(repositoryService.getCurrentRepository().getConfiguration().getDefaultWorkspaceName(),
            repositoryService.getCurrentRepository());

    Node spaceNewsRootNode = getSpaceNewsRootNode(news, session);

    Calendar creationCalendar = Calendar.getInstance();
    if(news.getCreationDate() != null) {
      creationCalendar.setTime(news.getCreationDate());
    } else {
      news.setCreationDate(creationCalendar.getTime());
    }

    Node newsFolderNode = dataDistributionType.getOrCreateDataNode(spaceNewsRootNode, getNodeRelativePath(creationCalendar));
    Node newsNode = newsFolderNode.addNode(Utils.cleanString(news.getTitle()), "exo:news");
    newsNode.addMixin("exo:datetime");
    newsNode.setProperty("exo:title", news.getTitle());
    newsNode.setProperty("exo:summary", news.getSummary());
    newsNode.setProperty("exo:body", news.getBody());
    newsNode.setProperty("exo:author", news.getAuthor());
    newsNode.setProperty("exo:dateCreated", creationCalendar);
    Calendar updateCalendar = Calendar.getInstance();
    if(news.getUpdateDate() != null) {
      updateCalendar.setTime(news.getUpdateDate());
    } else {
      news.setUpdateDate(updateCalendar.getTime());
    }
    newsNode.setProperty("exo:dateModified", updateCalendar);
    newsNode.setProperty("exo:pinned", false);
    newsNode.setProperty("exo:spaceId", news.getSpaceId());

    spaceNewsRootNode.save();

    if(StringUtils.isNotEmpty(news.getUploadId())) {
      attachIllustration(newsNode, news.getUploadId());
    }
    return newsNode.getUUID();
  }

  /**
   * Post the news activity in the given space
   * @param news The news to post as an activity
   */
  private void postNewsActivity(News news) {
    Identity poster = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, news.getAuthor(), false);

    Space space = spaceService.getSpaceById(news.getSpaceId());
    Identity spaceIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName(), false);

    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("");
    activity.setBody("");
    activity.setType("news");
    activity.setUserId(poster.getId());
    Map<String, String> templateParams = new HashMap<>();
    templateParams.put("newsId", news.getId());
    activity.setTemplateParams(templateParams);

    activityManager.saveActivityNoReturn(spaceIdentity, activity);
  }

  private String getNodeRelativePath(Calendar now) {
    return now.get(Calendar.YEAR) + "/" + (now.get(Calendar.MONTH) + 1) + "/" + now.get(Calendar.DAY_OF_MONTH);
  }

  private void attachIllustration(Node newsNode, String uploadId) throws Exception {
    UploadResource uploadedResource = uploadService.getUploadResource(uploadId);
    if (uploadedResource == null) {
      throw new Exception("Cannot attach uploaded file " + uploadId + ", it may not exist");
    }

    Node node = newsNode.addNode("illustration", "nt:file");
    node.setProperty("exo:title", uploadedResource.getFileName());
    Node resourceNode = node.addNode("jcr:content", "nt:resource");
    resourceNode.setProperty("jcr:mimeType", uploadedResource.getMimeType());
    resourceNode.setProperty("jcr:lastModified", Calendar.getInstance());
    String fileDiskLocation = uploadedResource.getStoreLocation();
    try(InputStream inputStream = new FileInputStream(fileDiskLocation)) {
      resourceNode.setProperty("jcr:data", inputStream);
      newsNode.save();
    }

    uploadService.removeUploadResource(uploadId);
  }

  private Node getSpaceNewsRootNode(News news, Session session) throws RepositoryException {
    Space space = spaceService.getSpaceById(news.getSpaceId());
    String groupPath = nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH);
    String spaceParentPath = groupPath + space.getGroupId();

    Node spaceRootNode = (Node) session.getItem(spaceParentPath);

    Node spaceNewsRootNode;
    if(!spaceRootNode.hasNode(NEWS_NODES_FOLDER)) {
      spaceNewsRootNode = spaceRootNode.addNode(NEWS_NODES_FOLDER, "nt:unstructured");
      if(spaceNewsRootNode.canAddMixin("exo:privilegeable")) {
        spaceNewsRootNode.addMixin("exo:privilegeable");
      }
      ((ExtendedNode) spaceNewsRootNode).setPermission("*:/platform/administrators", PermissionType.ALL);
      ((ExtendedNode) spaceNewsRootNode).setPermission("*:" + space.getGroupId(), PermissionType.ALL);
      spaceRootNode.save();
    } else {
      spaceNewsRootNode = spaceRootNode.getNode(NEWS_NODES_FOLDER);
    }
    return spaceNewsRootNode;
  }

  private News convertNodeToNews(Node node) throws Exception {
    if(node == null) {
      return null;
    }

    News news = new News();

    news.setId(node.getUUID());
    news.setTitle(node.getProperty("exo:title").getString());
    news.setSummary(node.getProperty("exo:summary").getString());
    news.setBody(node.getProperty("exo:body").getString());
    news.setPinned(node.getProperty("exo:pinned").getBoolean());
    news.setSpaceId(node.getProperty("exo:spaceId").getString());

    if(node.hasNode("illustration")) {
      Node illustrationContentNode = node.getNode("illustration").getNode("jcr:content");
      byte[] bytes = IOUtils.toByteArray(illustrationContentNode.getProperty("jcr:data").getStream());
      news.setIllustration(bytes);
      news.setIllustrationUpdateDate(illustrationContentNode.getProperty("jcr:lastModified").getDate().getTime());
    }

    return news;
  }
}
