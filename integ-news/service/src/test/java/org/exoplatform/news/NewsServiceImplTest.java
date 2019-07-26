package org.exoplatform.news;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;

import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionManager;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.exoplatform.news.model.News;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.upload.UploadService;

@RunWith(MockitoJUnitRunner.class)
public class NewsServiceImplTest {

  @Mock
  RepositoryService       repositoryService;

  @Mock
  SessionProviderService  sessionProviderService;

  @Mock
  NodeHierarchyCreator    nodeHierarchyCreator;

  @Mock
  DataDistributionManager dataDistributionManager;

  @Mock
  SpaceService            spaceService;

  @Mock
  ActivityManager         activityManager;

  @Mock
  IdentityManager         identityManager;

  @Mock
  UploadService           uploadService;

  @Test
  public void shouldGetNodeWhenNewsExists() throws Exception {
    // Given
    NewsService newsService = new NewsServiceImpl(repositoryService,
                                                  sessionProviderService,
                                                  nodeHierarchyCreator,
                                                  dataDistributionManager,
                                                  spaceService,
                                                  activityManager,
                                                  identityManager,
                                                  uploadService);
    ManageableRepository repository = mock(ManageableRepository.class);
    RepositoryEntry repositoryEntry = mock(RepositoryEntry.class);
    SessionProvider sessionProvider = mock(SessionProvider.class);
    Session session = mock(Session.class);
    Node node = mock(Node.class);
    Property property = mock(Property.class);
    when(sessionProviderService.getSystemSessionProvider(any())).thenReturn(sessionProvider);
    when(sessionProviderService.getSessionProvider(any())).thenReturn(sessionProvider);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repositoryEntry.getDefaultWorkspaceName()).thenReturn("collaboration");
    when(sessionProvider.getSession(any(), any())).thenReturn(session);
    when(session.getNodeByUUID(anyString())).thenReturn(node);
    when(node.getProperty(anyString())).thenReturn(property);

    // When
    News news = newsService.getNews("1");

    // Then
    assertNotNull(news);
  }

  @Test
  public void shouldGetNullWhenNewsDoesNotExist() throws Exception {
    // Given
    NewsService newsService = new NewsServiceImpl(repositoryService,
                                                  sessionProviderService,
                                                  nodeHierarchyCreator,
                                                  dataDistributionManager,
                                                  spaceService,
                                                  activityManager,
                                                  identityManager,
                                                  uploadService);
    ManageableRepository repository = mock(ManageableRepository.class);
    RepositoryEntry repositoryEntry = mock(RepositoryEntry.class);
    SessionProvider sessionProvider = mock(SessionProvider.class);
    Session session = mock(Session.class);
    when(sessionProviderService.getSystemSessionProvider(any())).thenReturn(sessionProvider);
    when(sessionProviderService.getSessionProvider(any())).thenReturn(sessionProvider);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repositoryEntry.getDefaultWorkspaceName()).thenReturn("collaboration");
    when(sessionProvider.getSession(any(), any())).thenReturn(session);
    when(session.getNodeByUUID(anyString())).thenReturn(null);

    // When
    News news = newsService.getNews("1");

    // Then
    assertNull(news);
  }
}
