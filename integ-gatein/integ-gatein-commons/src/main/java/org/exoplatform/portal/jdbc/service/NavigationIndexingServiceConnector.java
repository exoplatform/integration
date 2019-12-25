/*
 * Copyright (C) 2019 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.portal.jdbc.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.chromattic.ext.format.BaseEncodingObjectFormatter;
import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.commons.search.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.mop.Described;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.description.DescriptionService;
import org.exoplatform.portal.mop.navigation.NavigationData;
import org.exoplatform.portal.mop.navigation.NavigationStore;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.seo.PageMetadataModel;
import org.exoplatform.services.seo.SEOService;
import org.gatein.portal.controller.resource.script.Module;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class NavigationIndexingServiceConnector extends ElasticIndexingServiceConnector {

  private static final Log LOG = ExoLogger.getLogger(NavigationIndexingServiceConnector.class);

  private static final BaseEncodingObjectFormatter formatter = new BaseEncodingObjectFormatter();

  public final static String TYPE = "navigation";

  private NavigationStore navigationStore;

  private SEOService seoService;

  private PageService pageService;

  private DescriptionService descriptionService;

  public NavigationIndexingServiceConnector(InitParams initParams, NavigationStore navigationStore, SEOService seoService, PageService pageService, DescriptionService descriptionService) {
    super(initParams);
    this.navigationStore = navigationStore;
    this.seoService = seoService;
    this.pageService = pageService;
    this.descriptionService = descriptionService;
  }

  @Override
  public Document create(String nodeId) {
    if (StringUtils.isBlank(nodeId)) {
      throw new IllegalArgumentException("nodeId is mandatory");
    }

    long ts = System.currentTimeMillis();
    LOG.debug("get navigation ndoe document for node={}", nodeId);

    NodeData node = navigationStore.loadNode(Util.parseLong(nodeId));
    if (node == null) {
      LOG.warn("Node with id {} does not exist or has been removed", nodeId);
      return null;
    }
    NavigationData nav = this.navigationStore.loadNavigationData(Util.parseLong(node.getId()));
    String uri = getUri(node);

    Map<String, String> fields = new HashMap<>();
    fields.put("name", node.getName());
    fields.put("nodeId", node.getId());
    fields.put("siteName", nav.getSiteKey().getName());
    fields.put("siteType", nav.getSiteKey().getTypeName());

    //seo
    String seoMetadata = getSEO(node);
    if (seoMetadata != null) {
      fields.put("seo", seoMetadata);
    }
    //page
    Set<String> permissions = new HashSet<>();
    PageKey pageKey = node.getState().getPageRef();
    if (pageKey != null) {
      fields.put("pageRef", pageKey.format());
      PageContext page = pageService.loadPage(pageKey);

      String pageTitle = page.getState().getDisplayName();
      fields.put("pageTitle", pageTitle);

      permissions.addAll(page.getState().getAccessPermissions());
    }
    //description
    Map<Locale, Described.State> descriptions = descriptionService.getDescriptions(node.getId());
    if (descriptions != null && descriptions.size() > 0) {
      JSONObject json = new JSONObject();
      try {
        for (Locale locale : descriptions.keySet()) {
          Described.State state = descriptions.get(locale);
          json.put(locale.toLanguageTag(), state.getName());
        }
        fields.put("descriptions", json.toString());
      } catch (JSONException ex) {}
    }

    Date createdDate = new Date();
    Document document = new Document(TYPE, nodeId, uri, createdDate, permissions, fields);
    LOG.info("page document generated for node={} name={} duration_ms={}", nodeId, node.getName(), System.currentTimeMillis() - ts);

    return document;
  }

  private String getUri(NodeData node) {
    List<NodeData> nodes = new ArrayList<>();
    nodes.add(node);
    while (node.getParentId() != null) {
      node = navigationStore.loadNode(Util.parseLong(node.getParentId()));
      nodes.add(0, node);
    }
    // Remove the default node
    nodes.remove(0);

    // Build path
    List<String> paths = nodes.stream().map(n -> n.getName()).collect(Collectors.toList());
    return StringUtils.join(paths, "/");
  }

  private String getSEO(NodeData node) {
    try {
      NavigationData nav = this.navigationStore.loadNavigationData(Util.parseLong(node.getId()));
      String siteName = formatter.encodeNodeName(null, nav.getSiteKey().getName());
      final Map<String, PageMetadataModel> metaModels = seoService.getPageMetadatas(node.getId(), siteName);
      if (metaModels != null && metaModels.size() > 0) {
        JSONObject seo = new JSONObject();
        for(String key : metaModels.keySet()) {
          PageMetadataModel meta = metaModels.get(key);
          JSONObject json = new JSONObject();
          json.put("description", meta.getDescription());
          json.put("keywords", meta.getKeywords());
          json.put("title", meta.getTitle());
          json.put("robotContent", meta.getRobotsContent());
          seo.put(key, json);
        }
        return seo.toString();
      }
    } catch (Exception e) {
      LOG.error("Can not get SEO metadata of node " + node.getId(), e);
    }
    return null;
  }

  @Override
  public Document update(String id) {
    return create(id);
  }

  @Override
  public List<String> getAllIds(int offset, int limit) {
    List<String> ids = new LinkedList<>();
    ids.addAll(getNodes(navigationStore.loadNavigations(SiteType.PORTAL)));
    ids.addAll(getNodes(navigationStore.loadNavigations(SiteType.GROUP)));
    ids.addAll(getNodes(navigationStore.loadNavigations(SiteType.USER)));
    return ids;
  }

  private Collection<? extends String> getNodes(List<NavigationData> navigations) {
    List<String> ids = new ArrayList<>();
    for (NavigationData nav : navigations) {
      ids.addAll(getNodes(nav.getRootId()));
    }
    return ids;
  }

  private Collection<? extends String> getNodes(String rootId) {
    List<String> ids = new ArrayList<>();
    ids.add(rootId);
    NodeData node = navigationStore.loadNode(Util.parseLong(rootId));

    Iterator<String> nodes = node.iterator(false);
    while (nodes != null && nodes.hasNext()) {
      String childId = nodes.next();
      ids.addAll(getNodes(childId));
    }
    return ids;
  }

  @Override
  public String getMapping() {
    StringBuilder mapping = new StringBuilder()
            .append("{")
            .append("  \"properties\" : {\n")
            .append("    \"name\" : {")
            .append("      \"type\" : \"text\",")
            .append("      \"index_options\": \"offsets\",")
            .append("      \"fields\": {")
            .append("        \"raw\": {")
            .append("          \"type\": \"keyword\"")
            .append("        }")
            .append("      }")
            .append("    },\n")
            .append("    \"nodeId\" : {\"type\" : \"keyword\"},\n")
            .append("    \"siteName\": {\"type\" : \"keyword\"},\n")
            .append("    \"siteType\": {\"type\" : \"keyword\"},\n")
            .append("    \"pageRef\" : {\"type\" : \"keyword\"},\n")
            .append("    \"pageTitle\" : {\"type\" : \"text\", \"index_options\": \"offsets\"},\n")
            .append("    \"seo\" : {\"type\" : \"text\", \"index_options\": \"offsets\"},\n")
            .append("    \"descriptions\" : {\"type\" : \"text\", \"index_options\": \"offsets\"},\n")
            .append("    \"permissions\" : {\"type\" : \"keyword\"},\n")
            .append("    \"lastUpdatedDate\" : {\"type\" : \"date\", \"format\": \"epoch_millis\"}\n")
            .append("  }\n")
            .append("}");

    return mapping.toString();
  }

}
