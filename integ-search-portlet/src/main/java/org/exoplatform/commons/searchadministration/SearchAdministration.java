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


import juzu.Path;
import juzu.View;
import juzu.impl.request.Request;
import juzu.request.RenderContext;
import juzu.request.RequestContext;
import juzu.template.Template;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

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
  public void index(RenderContext renderContext){
    RequestContext requestContext = Request.getCurrent().getContext();
    
    ResourceBundle rs = renderContext.getApplicationContext().resolveBundle(renderContext.getUserContext().getLocale());
    Map<String, Object> parameters = new HashMap<String, Object>();

    parameters.put("searchadministration", rs.getString("searchadministration.label"));
    parameters.put("contentType", rs.getString("searchadministration.contentType.label"));
    parameters.put("description", rs.getString("searchadministration.description.label"));
    parameters.put("action", rs.getString("searchadministration.action.label"));
    parameters.put("disable", rs.getString("searchadministration.disable.label"));
    
    index.render(parameters);      
  }  
}
