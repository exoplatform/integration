package org.exoplatform.news.rest;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.RuntimeDelegate;

import org.exoplatform.services.rest.impl.RuntimeDelegateImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.exoplatform.news.NewsService;
import org.exoplatform.news.model.News;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

@RunWith(MockitoJUnitRunner.class)
public class NewsRestResourcesV1Test {

  @Mock
  NewsService newsService;

  @Mock
  SpaceService spaceService;

  @Mock
  IdentityManager identityManager;

  @Before
  public void setup() {
    RuntimeDelegate.setInstance(new RuntimeDelegateImpl());
  }

  @Test
  public void shouldGetNewsWhenNewsExistsAndUserIsMemberOfTheSpace() throws Exception {
    // Given
    NewsRestResourcesV1 newsRestResourcesV1 = new NewsRestResourcesV1(newsService, spaceService, identityManager);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn("john");
    News news = new News();
    news.setId("1");
    news.setIllustration("illustration".getBytes());
    when(newsService.getNews(anyString())).thenReturn(news);
    when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    when(spaceService.isMember(any(Space.class), eq("john"))).thenReturn(true);
    when(spaceService.isSuperManager(eq("john"))).thenReturn(false);

    // When
    Response response = newsRestResourcesV1.getNews(request, "1");

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    News fetchedNews = (News) response.getEntity();
    assertNotNull(fetchedNews);
    assertNull(fetchedNews.getIllustration());
  }

  @Test
  public void shouldGetNewsWhenNewsExistsAndUserIsNotMemberOfTheSpaceButSuperManager() throws Exception {
    // Given
    NewsRestResourcesV1 newsRestResourcesV1 = new NewsRestResourcesV1(newsService, spaceService, identityManager);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn("john");
    News news = new News();
    when(newsService.getNews(anyString())).thenReturn(news);
    when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    when(spaceService.isMember(any(Space.class), eq("john"))).thenReturn(false);
    when(spaceService.isSuperManager(eq("john"))).thenReturn(true);

    // When
    Response response = newsRestResourcesV1.getNews(request, "1");

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void shouldGetNotFoundWhenNewsExistsAndUserIsNotMemberOfTheSpaceNorSuperManager() throws Exception {
    // Given
    NewsRestResourcesV1 newsRestResourcesV1 = new NewsRestResourcesV1(newsService, spaceService, identityManager);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn("john");
    News news = new News();
    when(newsService.getNews(anyString())).thenReturn(news);
    when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    when(spaceService.isMember(any(Space.class), eq("john"))).thenReturn(false);
    when(spaceService.isSuperManager(eq("john"))).thenReturn(false);

    // When
    Response response = newsRestResourcesV1.getNews(request, "1");

    // Then
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  @Test
  public void shouldGetNotFoundWhenNewsNotExists() throws Exception {
    // Given
    NewsRestResourcesV1 newsRestResourcesV1 = new NewsRestResourcesV1(newsService, spaceService, identityManager);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn("john");
    when(newsService.getNews(anyString())).thenReturn(null);
    when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    when(spaceService.isMember(any(Space.class), eq("john"))).thenReturn(true);
    when(spaceService.isSuperManager(eq("john"))).thenReturn(true);

    // When
    Response response = newsRestResourcesV1.getNews(request, "1");

    // Then
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }

  @Test
  public void shouldGetOKWhenUpdatingNewsAndNewsExistsAndUserIsMemberOfTheSpace() throws Exception {
    // Given
    NewsRestResourcesV1 newsRestResourcesV1 = new NewsRestResourcesV1(newsService, spaceService, identityManager);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn("john");
    News news = new News();
    when(newsService.getNews(anyString())).thenReturn(news);
    when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    when(spaceService.isMember(any(Space.class), eq("john"))).thenReturn(true);
    when(spaceService.isSuperManager(eq("john"))).thenReturn(false);

    // When
    Response response = newsRestResourcesV1.updateNews(request, "1", new News());

    // Then
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void shouldGetNotAuthorizedWhenUpdatingNewsAndNewsExistsAndUserIsNotMemberOfTheSpaceNorSuperManager() throws Exception {
    // Given
    NewsRestResourcesV1 newsRestResourcesV1 = new NewsRestResourcesV1(newsService, spaceService, identityManager);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn("john");
    News news = new News();
    when(newsService.getNews(anyString())).thenReturn(news);
    when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    when(spaceService.isMember(any(Space.class), eq("john"))).thenReturn(false);
    when(spaceService.isSuperManager(eq("john"))).thenReturn(false);

    // When
    Response response = newsRestResourcesV1.updateNews(request, "1", new News());

    // Then
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
  }

  @Test
  public void shouldGetNotFoundWhenUpdatingNewsAndNewsNotExists() throws Exception {
    // Given
    NewsRestResourcesV1 newsRestResourcesV1 = new NewsRestResourcesV1(newsService, spaceService, identityManager);
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRemoteUser()).thenReturn("john");
    when(newsService.getNews(anyString())).thenReturn(null);
    when(spaceService.getSpaceById(anyString())).thenReturn(new Space());
    when(spaceService.isMember(any(Space.class), eq("john"))).thenReturn(false);
    when(spaceService.isSuperManager(eq("john"))).thenReturn(false);

    // When
    Response response = newsRestResourcesV1.updateNews(request, "1", new News());

    // Then
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
  }
}
