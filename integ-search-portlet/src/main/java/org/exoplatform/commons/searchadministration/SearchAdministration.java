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
package org.exoplatform.commons.searchadministration;


import java.util.ResourceBundle;

import javax.inject.Inject;

import juzu.Path;
import juzu.View;
import juzu.template.Template;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by The eXo Platform SAS
 * Author : Canh Pham Van
 *          canhpv@exoplatform.com
 * Jan 22, 2013  
 */
public class SearchAdministration {

  @Inject
  @Path("index.gtmpl")
  Template index;
  
  @Inject
  ResourceBundle bundle;  
    
  @View
  public void index(){
    Map<String, Object> parameters = new HashMap<String, Object>();
    Locale locale = bundle.getLocale();      
    ResourceBundle rs = ResourceBundle.getBundle("searchadministration/searchadministration", locale);
    parameters.put("searchadministration", rs.getString("searchadministration.label"));
    parameters.put("contentType", rs.getString("searchadministration.contentType.label"));
    parameters.put("description", rs.getString("searchadministration.description.label"));
    parameters.put("action", rs.getString("searchadministration.action.label"));
    parameters.put("disable", rs.getString("searchadministration.disable.label"));
    
    index.render(parameters);      
  }  
}
