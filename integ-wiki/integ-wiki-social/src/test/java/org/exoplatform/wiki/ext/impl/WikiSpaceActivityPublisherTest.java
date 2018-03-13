package org.exoplatform.wiki.ext.impl;

import com.google.gwt.activity.shared.Activity;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.rendering.RenderingService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiService;
import org.gatein.api.Portal;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
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

  @Test
  public void testSaveActivity() throws Exception {
    Page page = new Page();
    page.setContent("line1\n\nline2\n\nline3\n\nline4\n\nline5");

    //Creating mocks
    WikiService wikiService = Mockito.mock(WikiService.class);
    ActivityManager activityManager = Mockito.mock(ActivityManager.class);
    prepareMockServices(page, activityManager);

    //save activity
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(wikiService);
    wikiSpaceActivityPublisher.saveActivity("portal", "root", "page1", page, PageUpdateType.ADD_PAGE);
    //check if activity manager saveActivityNoReturn is called
    ArgumentCaptor<ExoSocialActivity> activityCaptor = ArgumentCaptor.forClass(ExoSocialActivity.class);
    Mockito.verify(activityManager).saveActivityNoReturn(Mockito.any(org.exoplatform.social.core.identity.model.Identity.class), activityCaptor.capture());
    ExoSocialActivity activity = activityCaptor.getValue();

    //check the exceprt is processed correctly
    //only get first 4 lines of wiki page WIKI-1290
    String exceprt = activity.getTemplateParams().get("page_exceprt");
    Assert.assertEquals("<p>line1</p><p>line2</p><p>line3</p><p>line4</p>...", exceprt);

    //case 2: there are only 4 not empty lines, the ellipsis should not be added
    page.setContent("line1\n\nline2\n\n  \n\nline3\n\nline4");
    activityManager = Mockito.mock(ActivityManager.class);
    prepareMockServices(page, activityManager);
    wikiSpaceActivityPublisher.saveActivity("portal", "root", "page1", page, PageUpdateType.ADD_PAGE);
    //capture new activity
    activityCaptor = ArgumentCaptor.forClass(ExoSocialActivity.class);
    Mockito.verify(activityManager).saveActivityNoReturn(Mockito.any(org.exoplatform.social.core.identity.model.Identity.class), activityCaptor.capture());
    activity = activityCaptor.getValue();
    //
    exceprt = activity.getTemplateParams().get("page_exceprt");
    Assert.assertEquals("<p>line1</p><p>line2</p><p>line3</p><p>line4</p>", exceprt);
  }

  private void prepareMockServices(Page page, ActivityManager activityManager) throws Exception {
    ConversationState state = Mockito.mock(ConversationState.class);
    Identity owner = new Identity("root");
    Mockito.when(state.getIdentity()).thenReturn(owner);
    ConversationState.setCurrent(state);

    Mockito.when(activityManager.getActivity(Mockito.anyString())).thenReturn(null);

    IdentityManager identityManager = Mockito.mock(IdentityManager.class);
    Mockito.when(identityManager.getOrCreateIdentity(Mockito.anyString(),
            Mockito.anyString(), Mockito.anyBoolean())).thenReturn(Mockito.mock(org.exoplatform.social.core.identity.model.Identity.class));
    //
    RenderingService renderingService = Mockito.mock(RenderingService.class);
    Mockito.when(renderingService.render(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean())).thenReturn(page.getContent());

    PortalContainer portalCont = Mockito.mock(PortalContainer.class);
    PortalContainer.setInstance(portalCont);
    Mockito.when(portalCont.getComponentInstanceOfType(IdentityManager.class)).thenReturn(identityManager);
    Mockito.when(portalCont.getComponentInstanceOfType(ActivityManager.class)).thenReturn(activityManager);
    Mockito.when(portalCont.getComponentInstanceOfType(RenderingService.class)).thenReturn(renderingService);
  }
}