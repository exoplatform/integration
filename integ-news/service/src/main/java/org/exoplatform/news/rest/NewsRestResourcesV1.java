package org.exoplatform.news.rest;

import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.*;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.news.NewsService;
import org.exoplatform.news.model.News;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.model.ActivityStream;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;

import io.swagger.annotations.*;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;

@Path("v1/news")
@Api(tags = "v1/news", value = "v1/news", description = "Managing news")
public class NewsRestResourcesV1 implements ResourceContainer {

  private static final Log LOG = ExoLogger.getLogger(NewsRestResourcesV1.class);

  private NewsService newsService;

  private SpaceService spaceService;

  public NewsRestResourcesV1(NewsService newsService, SpaceService spaceService) {
    this.newsService = newsService;
    this.spaceService = spaceService;
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Create a news",
          httpMethod = "POST",
          response = Response.class,
          notes = "This creates the news if the authenticated user is a member of the space or a spaces super manager.")
  @ApiResponses(value = {
          @ApiResponse (code = 200, message = "News created"),
          @ApiResponse (code = 400, message = "Invalid query input"),
          @ApiResponse (code = 401, message = "User not authorized to create the news"),
          @ApiResponse (code = 500, message = "Internal server error")})
  public Response createNews(@ApiParam(value = "News", required = true) News news) {
    if (news == null || StringUtils.isEmpty(news.getSpaceId())) {
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();

    Space space = spaceService.getSpaceById(news.getSpaceId());
    if (space == null || (! spaceService.isMember(space, authenticatedUser) && ! spaceService.isSuperManager(authenticatedUser))) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    try {
      News createdNews = newsService.createNews(news);

      return Response.ok(createdNews).build();
    } catch (Exception e) {
      LOG.error("Error when creating the news " + news.getTitle(), e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path("{id}/illustration")
  @RolesAllowed("users")
  @ApiOperation(value = "Get a news",
          httpMethod = "GET",
          response = Response.class,
          notes = "This gets the news with the given id if the authenticated user is a member of the space or a spaces super manager.")
  @ApiResponses(value = {
          @ApiResponse (code = 200, message = "News returned"),
          @ApiResponse (code = 401, message = "User not authorized to get the news"),
          @ApiResponse (code = 404, message = "News not found"),
          @ApiResponse (code = 500, message = "Internal server error") })
  public Response getNewsIllustration(@Context Request request,
                                      @ApiParam(value = "News id", required = true) @PathParam("id") String id) {
    try {
      News news = newsService.getNews(id);
      if (news == null || news.getIllustration() == null || news.getIllustration().length == 0) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }

      String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();

      Space space = spaceService.getSpaceById(news.getSpaceId());
      if (space == null || (! spaceService.isMember(space, authenticatedUser) && ! spaceService.isSuperManager(authenticatedUser))) {
        throw new WebApplicationException(Response.Status.UNAUTHORIZED);
      }

      EntityTag eTag = new EntityTag(String.valueOf(news.getIllustrationUpdateDate().getTime()));
      //
      Response.ResponseBuilder builder = (eTag == null ? null : request.evaluatePreconditions(eTag));
      if (builder == null) {
        builder = Response.ok(news.getIllustration(), "image/png");
        builder.tag(eTag);
      }

      return builder.build();
    } catch (Exception e) {
      LOG.error("Error when getting the news " + id, e);
      return Response.serverError().build();
    }
  }

  @POST
  @Path("{id}/click")
  @RolesAllowed("users")
  @ApiOperation(value = "Click on read more or news title", httpMethod = "POST", response = Response.class, notes = "This will display a log message when the user click on read more or the title of a news")
  @ApiResponses(value = { @ApiResponse(code = 204, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error"), @ApiResponse(code = 403, message = "Invalid query input") })
  public Response clickOnNews(@Context UriInfo uriInfo,
                              @ApiParam(value = "Activity id", required = true) @PathParam("id") String id,
                              @ApiParam(value = "The target cliked field", required = true) Map<String, String> targetField) {

    String authenticatedUser = ConversationState.getCurrent().getIdentity().getUserId();
    Identity currentUser = CommonsUtils.getService(IdentityManager.class)
                                       .getOrCreateIdentity(OrganizationIdentityProvider.NAME, authenticatedUser, true);

    ActivityManager activityManager = CommonsUtils.getService(ActivityManager.class);
    ExoSocialActivity activity = activityManager.getActivity(id);
    if (activity == null) {
      throw new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    ActivityStream activityStream = activity.getActivityStream();
    if ("news".equals(activity.getType()) && activityStream != null
        && activityStream.getType().equals(ActivityStream.Type.SPACE)) {
      LOG.info("service=news operation=click_on_{} parameters=\"activity_id:{},space_name:{},space_id:{},user_id:{}\"",
               targetField.get("name"),
               activity.getId(),
               activityStream.getPrettyId(),
               activityStream.getId(),
               currentUser.getId());
    }
    return Response.status(Response.Status.OK).build();
  }

}
