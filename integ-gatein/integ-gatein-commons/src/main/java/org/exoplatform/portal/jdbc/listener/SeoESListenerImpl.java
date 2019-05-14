package org.exoplatform.portal.jdbc.listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.search.es.ElasticSearchException;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.portal.jdbc.service.NavigationIndexingServiceConnector;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.navigation.GenericScope;
import org.exoplatform.portal.mop.navigation.NavigationContext;
import org.exoplatform.portal.mop.navigation.NavigationService;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeModel;
import org.exoplatform.portal.mop.navigation.Scope;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class SeoESListenerImpl extends Listener<SEOService, PageMetadataModel> {

  private static final Log LOG = ExoLogger.getExoLogger(SeoESListenerImpl.class);

  private ElasticSearchingClient client;

  private IndexingService indexingService;

  private NavigationService navigationService;

  public SeoESListenerImpl(ElasticSearchingClient client, IndexingService indexingService, NavigationService navigationService) {
    this.client = client;
    this.indexingService = indexingService;
    this.navigationService = navigationService;
  }

  @Override
  public void onEvent(Event<SEOService, PageMetadataModel> event) throws Exception {
    PageMetadataModel seo = event.getData();
    updateIndex(seo);
  }

  private void updateIndex(PageMetadataModel seo) {
    for (String id : search(seo)) {
      indexingService.reindex(NavigationIndexingServiceConnector.TYPE, id);
    }
  }

  public List<String> search(PageMetadataModel seo) {
    SiteKey siteKey = seo.getSiteKey();
    NavigationContext context = navigationService.loadNavigation(seo.getSiteKey());
    NodeContext node = navigationService.loadNode(NodeModel.SELF_MODEL, context, GenericScope.branchShape(seo.getUri().split("/")), null);
    String esQuery = buildFilteredQuery(node.getId(), Arrays.asList(siteKey.toString()));
    String jsonResponse = this.client.sendRequest(esQuery, null, NavigationIndexingServiceConnector.TYPE);
    List<String> ids = buildResult(jsonResponse);
    return ids;
  }

  protected String buildFilteredQuery(String query, Collection<String> sites) {
    String escapedQuery = escapeReservedCharacters(query);

    StringBuilder esQuery = new StringBuilder();
    esQuery.append("{\n");

    //Score are always tracked, even with sort
    //https://www.impl.co/guide/en/elasticsearch/reference/current/search-request-sort.html#_track_scores
    esQuery.append("     \"track_scores\": true,\n");
    esQuery.append("     \"_source\": [nodeId],");
    esQuery.append("     \"query\": {\n");
    esQuery.append("        \"bool\" : {\n");
    esQuery.append("            \"must\" : {\n");
    esQuery.append("                \"query_string\" : {\n");
    esQuery.append("                    \"fields\" : [nodeId],\n");
    esQuery.append("                    \"query\" : \"" + escapedQuery + "\"\n");
    esQuery.append("                }\n");
    esQuery.append("            },\n");
    esQuery.append("            \"filter\" : {\n");
    esQuery.append("              \"bool\" : {\n");
    esQuery.append("                \"must\" : [\n");
    String sitesFilter = getSitesFilter(sites);
    if (StringUtils.isNotBlank(sitesFilter)) {
      esQuery.append("                  ,{\n");
      esQuery.append("                   \"bool\" : {\n");
      esQuery.append("                     \"should\" : \n");
      esQuery.append("                      " + sitesFilter + "\n");
      esQuery.append("                    }\n");
      esQuery.append("                  }");
    }
    esQuery.append("                  \n");
    esQuery.append("                ]\n");
    esQuery.append("              }\n");
    esQuery.append("            }");
    esQuery.append("        }\n");
    esQuery.append("     }\n");
    esQuery.append("}");

    LOG.debug("Search Query request to ES : {} ", esQuery);

    return esQuery.toString();
  }

  protected String getSitesFilter(Collection<String> sitesCollection) {
    if ((sitesCollection != null) && (sitesCollection.size() > 0)) {
      List<String> sites = new ArrayList<>();
      for (String site : sitesCollection) {
        sites.add("\"" + site + "\"");
      }
      String sitesList = "[" + StringUtils.join(sites, ",") + "]";
      return " [ { \"bool\" : {\n" +
              "         \"must_not\": {\n" +
              "           \"exists\" : { \"field\" : \"sites\" }\n" +
              "         }\n" +
              "       }\n" +
              "},\n" +
              "{\n" +
              "  \"terms\" : { \n" +
              "    \"sites\" : " + sitesList + "\n" +
              "  }\n" +
              "} ]";
    } else {
      return " { \"bool\" : " +
              "{\n" +
              "  \"must_not\": {\n" +
              "      \"exists\" : { \"field\" : \"sites\" }\n" +
              "   }\n" +
              "  }\n" +
              "}\n";
    }
  }

  protected String escapeReservedCharacters(String query) {
    if (StringUtils.isNotEmpty(query)) {
      return query.replaceAll("[" + Pattern.quote("+-=&|><!(){}\\[\\]^\"*?:\\/") + "]",
              Matcher.quoteReplacement("\\\\") + "$0");
    } else {
      return query;
    }
  }

  protected List<String> buildResult(String jsonResponse) {

    LOG.debug("Search Query response from ES : {} ", jsonResponse);

    List<String> results = new ArrayList<>();
    JSONParser parser = new JSONParser();

    Map json;
    try {
      json = (Map) parser.parse(jsonResponse);
    } catch (ParseException e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }

    JSONObject jsonResult = (JSONObject) json.get("hits");
    if (jsonResult != null) {
      JSONArray jsonHits = (JSONArray) jsonResult.get("hits");

      if (jsonHits != null) {
        for (Object jsonHit : jsonHits) {
          results.add((String) ((JSONObject) jsonHit).get("id"));
        }
      }
    }

    return results;

  }

}
