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
import juzu.Response;
import juzu.View;
import juzu.bridge.portlet.JuzuPortlet;
import juzu.impl.request.Request;
import juzu.request.ApplicationContext;
import juzu.request.RequestContext;
import juzu.request.SecurityContext;
import juzu.request.UserContext;
import juzu.template.Template;

import javax.inject.Inject;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
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
  PortletPreferences portletPreferences;
  
  @Inject
  ResourceBundle bundle;    

  static boolean firstInit = true;
  
  @View
  public Response.Content index(RequestContext requestContext){
    Map<String, Object> parameters = new HashMap<String, Object>();        
    
    Search_.index().setProperty(JuzuPortlet.PORTLET_MODE, PortletMode.EDIT);
    PortletMode mode = requestContext.getProperty(JuzuPortlet.PORTLET_MODE);
    parameters.put("firstInit", firstInit);
    if (firstInit) firstInit = false;
    if (PortletMode.EDIT == mode){
      return edit.ok(parameters);
    } else {
      
      String resultsPerPage = portletPreferences.getValue("resultsPerPage", "10");
      String searchTypes = portletPreferences.getValue("searchTypes", "all");
      String searchCurrentSiteOnly = portletPreferences.getValue("searchCurrentSiteOnly", "false");
      String hideSearchForm = portletPreferences.getValue("hideSearchForm", "false");
      String hideFacetsFilter = portletPreferences.getValue("hideFacetsFilter", "false");    
      
      parameters.put("resultsPerPage", resultsPerPage);
      parameters.put("searchTypes", searchTypes);
      parameters.put("searchCurrentSiteOnly", searchCurrentSiteOnly);
      parameters.put("hideSearchForm", hideSearchForm);
      parameters.put("hideFacetsFilter", hideFacetsFilter);
      
      return index.ok(parameters);
    }
  }  
}
