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
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.commons.search.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.NavigationData;
import org.exoplatform.portal.mop.navigation.NavigationStore;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NavigationIndexingServiceConnector extends ElasticIndexingServiceConnector {

  private static final Log LOG = ExoLogger.getLogger(NavigationIndexingServiceConnector.class);

  public final static String TYPE = "navigation";

  private NavigationStore navigationStore;

  public NavigationIndexingServiceConnector(InitParams initParams, NavigationStore navigationStore) {
    super(initParams);
    this.navigationStore = navigationStore;
  }

  @Override
  public Document create(String nodeId) {
    if (StringUtils.isBlank(nodeId)) {
      throw new IllegalArgumentException("nodeId is mandatory");
    }

    long ts = System.currentTimeMillis();
    LOG.debug("get navigation ndoe document for node={}", nodeId);

    NodeData node = navigationStore.loadNode(nodeId);

    Map<String, String> fields = new HashMap<>();
    fields.put("name", node.getName());

    Date createdDate = new Date();

    Document document = new Document(TYPE, nodeId, null, createdDate, new HashSet<>(), fields);
    LOG.info("page document generated for node={} name={} duration_ms={}", nodeId, node.getName(), System.currentTimeMillis() - ts);

    return document;
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
    NodeData node = navigationStore.loadNode(rootId);

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
            .append("    \"pageRef\" : {\"type\" : \"text\", \"index_options\": \"offsets\"},\n")
            .append("    \"pageTitle\" : {\"type\" : \"text\", \"index_options\": \"offsets\"},\n")
            .append("    \"permissions\" : {\"type\" : \"keyword\"},\n")
            .append("    \"lastUpdatedDate\" : {\"type\" : \"date\", \"format\": \"epoch_millis\"}\n")
            .append("  }\n")
            .append("}");

    return mapping.toString();
  }

}
