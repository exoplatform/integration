/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.commons.unifiedsearch;


import juzu.Path;
import juzu.View;
import juzu.bridge.portlet.JuzuPortlet;
import juzu.impl.request.Request;
import juzu.request.RenderContext;
import juzu.request.RequestContext;
import juzu.template.Template;

import javax.inject.Inject;
import javax.portlet.PortletMode;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Created by The eXo Platform SAS
 * Author : Canh Pham Van
 *          canhpv@exoplatform.com
 * Nov 26, 2012  
 */
public class Search {

  @Inject
  @Path("index.gtmpl")
  Template index;
  
  @Inject
  @Path("edit.gtmpl")
  Template edit;
  
  @Inject
  ResourceBundle bundle;    
  
  @View
  public void index(RenderContext renderContext){
    RequestContext requestContext = Request.getCurrent().getContext();
    
    ResourceBundle rs = renderContext.getApplicationContext().resolveBundle(renderContext.getUserContext().getLocale());
    Map<String, Object> parameters = new HashMap<String, Object>();
    
    Search_.index().setProperty(JuzuPortlet.PORTLET_MODE, PortletMode.EDIT);
    PortletMode mode = requestContext.getProperty(JuzuPortlet.PORTLET_MODE);
    if (PortletMode.EDIT == mode){      
      parameters.put("unifiedsearch", rs.getString("unifiedsearch.edit.label"));
      parameters.put("resultsPerPage", rs.getString("unifiedsearch.edit.resultsPerPage.label"));
      parameters.put("searchIn", rs.getString("unifiedsearch.edit.searchIn.label"));
      parameters.put("currentsite", rs.getString("unifiedsearch.edit.currentsite.label"));
      parameters.put("hideSearchForm", rs.getString("unifiedsearch.edit.hideSearchForm.label"));
      parameters.put("hideFacetsFilter", rs.getString("unifiedsearch.edit.hideFacetsFilter.label"));
      parameters.put("saveSettings", rs.getString("unifiedsearch.edit.saveSettings.label"));
      parameters.put("everything", rs.getString("unifiedsearch.edit.everything.label"));
      parameters.put("alertOk", rs.getString("unifiedsearch.edit.alert.saveSettings"));
      parameters.put("alertNotOk", rs.getString("unifiedsearch.edit.alert.error.saveSettings"));
      
      edit.render(parameters);
    }else {
      parameters.put("unifiedsearch", rs.getString("unifiedsearch.index.label"));
      parameters.put("relevancy", rs.getString("unifiedsearch.index.relevancy.label"));
      parameters.put("date", rs.getString("unifiedsearch.index.date.label"));
      parameters.put("title", rs.getString("unifiedsearch.index.title.label"));
      parameters.put("sortBy", rs.getString("unifiedsearch.index.sortBy.label"));
      parameters.put("filterBy", rs.getString("unifiedsearch.index.filterBy.label"));
      parameters.put("allsites", rs.getString("unifiedsearch.index.allsites.label"));
      parameters.put("contentTypes", rs.getString("unifiedsearch.index.contentTypes.label"));
      parameters.put("showmore", rs.getString("unifiedsearch.index.showmore.label"));      
      parameters.put("searching", rs.getString("unifiedsearch.searching.label"));
      
      index.render(parameters);      
    }
  }  
}
