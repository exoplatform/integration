package org.exoplatform.commons.search.driver.jcr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.exoplatform.commons.api.search.SearchService;
import org.exoplatform.commons.api.search.SearchServiceConnector;
import org.exoplatform.commons.api.search.data.SearchContext;
import org.exoplatform.commons.api.search.data.SearchResult;
import org.exoplatform.commons.search.service.UnifiedSearchService;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class JcrSearchDriver extends SearchService {
  private final static Log LOG = ExoLogger.getLogger(JcrSearchDriver.class);
  private UserPortalConfigService userPortalConfigService;
  
  public JcrSearchDriver(UserPortalConfigService userPortalConfigService) {
    this.userPortalConfigService = userPortalConfigService;
  }
  
  @Override
  public Map<String, Collection<SearchResult>> search(SearchContext context, String query, Collection<String> sites, Collection<String> types, int offset, int limit, String sort, String order) {
    HashMap<String, ArrayList<String>> terms = parse(query); //parse query for single and quoted terms
    query = repeat("\"%s\"", terms.get("quoted"), " ") + " " + repeat("%s~", terms.get("single"), " "); //add a ~ after each single term (for fuzzy search)

    if(sites.contains("all")){
      try {
        sites = userPortalConfigService.getAllPortalNames();
      } catch (Exception e) {
        sites = new ArrayList<String>();
        LOG.error(e.getMessage(), e);
      }
    }
    
    Map<String, Collection<SearchResult>> results = new HashMap<String, Collection<SearchResult>>();
    if(null==types || types.isEmpty()) return results;
    List<String> enabledTypes = UnifiedSearchService.getEnabledSearchTypes();
    for(SearchServiceConnector connector:this.getConnectors()){
      if(!enabledTypes.contains(connector.getSearchType())) continue; //ignore disabled types
      if(!types.contains("all") && !types.contains(connector.getSearchType())) continue; //search requested types only
      LOG.debug("\n[UNIFIED SEARCH]: connector = " + connector.getClass().getSimpleName());
      try {
        results.put(connector.getSearchType(), connector.search(context, query, sites, offset, limit, sort, order));
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
        continue; //skip this connector and continue searching with the others
      }
    }
    return results;    
  }

  
  private static HashMap<String, ArrayList<String>> parse(String input) {
    HashMap<String, ArrayList<String>> terms = new HashMap<String, ArrayList<String>>();
    
    ArrayList<String> quoted = new ArrayList<String>();    
    Matcher matcher = Pattern.compile("\"([^\"]+)\"").matcher(input);
    while (matcher.find()) {
      String founds = matcher.group(1);
      quoted.add(founds);
    }
    terms.put("quoted", quoted);
    
    String remain = matcher.replaceAll("").replaceAll("\"", "").trim(); //remove all remaining double quotes
    ArrayList<String> single = new ArrayList<String>();
    if(!remain.isEmpty()) single.addAll(Arrays.asList(remain.split("\\s+")));
    terms.put("single", single);
    
    return terms;
  }
  
  private static String repeat(String format, Collection<String> strArr, String delimiter){
    StringBuilder sb=new StringBuilder();
    String delim = "";
    for(String str:strArr) {
      sb.append(delim).append(String.format(format, str));
      delim = delimiter;
    }
    return sb.toString();
  }

}
