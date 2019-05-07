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

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.commons.search.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.mop.QueryResult;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class PageIndexingServiceConnector extends ElasticIndexingServiceConnector {

  private static final Log LOG = ExoLogger.getLogger(PageIndexingServiceConnector.class);

  public final static String TYPE = "page";

  private PageService pageService;

  public PageIndexingServiceConnector(InitParams initParams, PageService pageService) {
    super(initParams);
    this.pageService = pageService;
  }

  @Override
  public Document create(String pageKey) {
    if (StringUtils.isBlank(pageKey)) {
      throw new IllegalArgumentException("id is mandatory");
    }

    long ts = System.currentTimeMillis();
    LOG.debug("get page document for pageKey={}", pageKey);

    PageState page = pageService.loadPage(PageKey.parse(pageKey)).getState();

    Map<String, String> fields = new HashMap<>();
    fields.put("displayName", page.getDisplayName());
    fields.put("description", page.getDescription());

    Date createdDate = new Date();

    Document document = new Document(TYPE, pageKey, null, createdDate, new HashSet<>(page.getAccessPermissions()), fields);
    LOG.info("page document generated for pageKey={} name={} duration_ms={}", pageKey, page.getDisplayName(), System.currentTimeMillis() - ts);

    return document;
  }

  @Override
  public Document update(String id) {
    return create(id);
  }

  @Override
  public List<String> getAllIds(int offset, int limit) {

    List<String> ids = new LinkedList<>();
    try {
      QueryResult<PageContext> results = pageService.findPages(offset, limit, null, null, null, null);
      if (results != null) {
        results.forEach(page -> {
          ids.add(page.getKey().format());
        });
      }
    } catch (Exception ex) {
      LOG.error(ex);
    }
    return ids;
  }

  @Override
  public String getMapping() {
    StringBuilder mapping = new StringBuilder()
            .append("{")
            .append("  \"properties\" : {\n")
            .append("    \"displayName\" : {")
            .append("      \"type\" : \"text\",")
            .append("      \"index_options\": \"offsets\",")
            .append("      \"fields\": {")
            .append("        \"raw\": {")
            .append("          \"type\": \"keyword\"")
            .append("        }")
            .append("      }")
            .append("    },\n")
            .append("    \"description\" : {\"type\" : \"text\", \"index_options\": \"offsets\"},\n")
            .append("    \"permissions\" : {\"type\" : \"keyword\"},\n")
            .append("    \"lastUpdatedDate\" : {\"type\" : \"date\", \"format\": \"epoch_millis\"}\n")
            .append("  }\n")
            .append("}");

    return mapping.toString();
  }

}
