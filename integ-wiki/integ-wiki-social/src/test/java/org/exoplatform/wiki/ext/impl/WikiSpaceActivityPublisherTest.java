package org.exoplatform.wiki.ext.impl;

import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiService;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test class for WikiSpaceActivityPublisher
 */
public class WikiSpaceActivityPublisherTest {
  @Test
  public void shouldNotCreateActivityWhenUpdateTypeIsNull() throws Exception {
    // Given
    WikiService wikiService = Mockito.mock(WikiService.class);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(wikiService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = Mockito.spy(wikiSpaceActivityPublisher);
    Page page = new Page();

    // When
    wikiSpaceActivityPublisher.postUpdatePage("portal", "portal1", "page1", page, null);

    // Then
    Mockito.verify(wikiSpaceActivityPublisherSpy, Mockito.never()).saveActivity("portal", "portal1", "page1", page, null);
  }

  @Test
  public void shouldNotCreateActivityWhenUpdateTypeIsPermissionsChange() throws Exception {
    // Given
    WikiService wikiService = Mockito.mock(WikiService.class);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(wikiService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = Mockito.spy(wikiSpaceActivityPublisher);
    Page page = new Page();

    // When
    wikiSpaceActivityPublisher.postUpdatePage("portal", "portal1", "page1", page, PageUpdateType.EDIT_PAGE_PERMISSIONS);

    // Then
    Mockito.verify(wikiSpaceActivityPublisherSpy, Mockito.never()).saveActivity("portal", "portal1", "page1", page, null);
  }

}