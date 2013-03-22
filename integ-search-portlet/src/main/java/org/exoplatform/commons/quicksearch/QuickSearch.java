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
package org.exoplatform.commons.quicksearch;


import juzu.Path;
import juzu.View;
import juzu.bridge.portlet.JuzuPortlet;
import juzu.impl.request.Request;
import juzu.request.RequestContext;
import juzu.template.Template;

import javax.inject.Inject;
import javax.portlet.PortletMode;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by The eXo Platform SAS
 * Author : Canh Pham Van
 *          canhpv@exoplatform.com
 * Nov 26, 2012  
 */
public class QuickSearch {

  @Inject
  @Path("index.gtmpl")
  Template index;
  
  @Inject
  @Path("edit.gtmpl")
  Template edit;
  
  @Inject
  ResourceBundle bundle;  
  
  @View
  public void index(){
    RequestContext requestContext = Request.getCurrent().getContext();
    Locale locale = bundle.getLocale(); 
    ResourceBundle rs = ResourceBundle.getBundle("quicksearch/quicksearch", locale);
    
    Map<String, Object> parameters = new HashMap<String, Object>();
    QuickSearch_.index().setProperty(JuzuPortlet.PORTLET_MODE, PortletMode.EDIT);
    PortletMode mode = requestContext.getProperty(JuzuPortlet.PORTLET_MODE);
    if (PortletMode.EDIT == mode){     
      parameters.put("quicksearch", rs.getString("quicksearch.label"));
      parameters.put("resultsPerType", rs.getString("quicksearch.resultsPerType.label"));
      parameters.put("searchIn", rs.getString("quicksearch.searchIn.label"));
      parameters.put("everything", rs.getString("quicksearch.everything.label"));
      parameters.put("currentsite", rs.getString("quicksearch.currentsite.label"));
      parameters.put("saveSettings", rs.getString("quicksearch.saveSettings.label"));
      parameters.put("alertOk", rs.getString("quicksearch.alert.saveSetting"));
      parameters.put("alertNotOk", rs.getString("quicksearch.alert.error.saveSetting"));
      edit.render(parameters);
    }else {
      parameters.put("SearchInInput", rs.getString("quicksearch.input.label"));
      parameters.put("seeAll", rs.getString("quicksearch.seeAll.label"));
      parameters.put("noResults", rs.getString("quicksearch.noResults.label"));      
      index.render(parameters);
    }
  }  
}
