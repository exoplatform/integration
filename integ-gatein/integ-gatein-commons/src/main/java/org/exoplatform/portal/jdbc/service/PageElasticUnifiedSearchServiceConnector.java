/*
 * Copyright (C) 2019 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/ .
 */
package org.exoplatform.portal.jdbc.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.search.es.ElasticSearchException;
import org.exoplatform.commons.search.es.ElasticSearchServiceConnector;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PageElasticUnifiedSearchServiceConnector extends ElasticSearchServiceConnector {

  private static final Log LOG = ExoLogger.getLogger(PageElasticUnifiedSearchServiceConnector.class);

  private PageService pageService;

  public PageElasticUnifiedSearchServiceConnector(InitParams initParams,
                                                  ElasticSearchingClient client,
                                                  PageService pageService) {
    super(initParams, client);
    this.pageService = pageService;
  }

  @Override
  protected String getSourceFields() {
    List<String> fields = new ArrayList<>();
    fields.add("displayName");
    fields.add("description");

    List<String> sourceFields = new ArrayList<>();
    for (String sourceField : fields) {
      sourceFields.add("\"" + sourceField + "\"");
    }

    return StringUtils.join(sourceFields, ",");
  }

  protected Collection<SearchResult> buildResult(String jsonResponse, SearchContext context) {

    LOG.debug("Search Query response from ES : {} ", jsonResponse);

    Collection<SearchResult> results = new ArrayList<>();
    JSONParser parser = new JSONParser();

    Map json;
    try {
      json = (Map) parser.parse(jsonResponse);
    } catch (ParseException e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }

    JSONObject jsonResult = (JSONObject) json.get("hits");
    if (jsonResult == null) {
      return results;
    }

    JSONArray jsonHits = (JSONArray) jsonResult.get("hits");

    for (Object jsonHit : jsonHits) {
      JSONObject hitSource = (JSONObject) ((JSONObject) jsonHit).get("_source");
      PageContext page = pageService.loadPage(PageKey.parse(((JSONObject) jsonHit).get("_id").toString()));

      String title = getTitleFromJsonResult(hitSource);
      String url = getUrlFromJsonResult(page, context);
      Long lastUpdatedDate = (Long) hitSource.get("lastUpdatedDate");
      if (lastUpdatedDate == null) lastUpdatedDate = new Date().getTime();
      Double score = (Double) ((JSONObject) jsonHit).get("_score");

      results.add(new SearchResult(
              url,
              title,
              page.getState().getDescription(),
              page.getState().getDescription(),
              "",
              lastUpdatedDate,
              //score must not be null as "track_scores" is part of the query
              score.longValue()
      ));
    }

    return results;

  }

  private String getUrlFromJsonResult(PageContext page, SearchContext context) {
//    try {
//      String permanentSpaceName = space.getPrettyName();
//      String groupId = space.getGroupId();
//
//      //
//      String siteName = groupId.replaceAll("/", ":");
//      String siteType = SiteType.GROUP.getName();
//
//      ExoContainerContext eXoContext = (ExoContainerContext) ExoContainerContext.getCurrentContainer()
//              .getComponentInstanceOfType(ExoContainerContext.class);
//      String portalName = eXoContext.getPortalContainerName();
//
//      String spaceURI = context.handler(portalName)
//              .lang("")
//              .siteName(siteName)
//              .siteType(siteType)
//              .path(permanentSpaceName)
//              .renderLink();
//      return URLDecoder.decode(String.format("/%s%s", portalName, spaceURI), "UTF-8");
//    } catch (Exception e) {
//      LOG.error("Cannot compute space url for " + space.getDisplayName(), e);
//      return "";
//    }
    return "";
  }

}
