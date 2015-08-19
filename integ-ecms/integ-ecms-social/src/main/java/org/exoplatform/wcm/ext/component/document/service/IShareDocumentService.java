/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
package org.exoplatform.wcm.ext.component.document.service;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 27, 2014  
 */
public interface IShareDocumentService {
  /**
   * Share a document to a space, permission of space's user also 
   * apply to this document. 
   * <p>
   * There is a symbolic link of origin document will create 
   * at Documents/Shared folder of destination space. 
   *
   * @param  space  destination space will share file in
   * @param  node file will be shared
   * @param  comment message attach with share activity
   * @param  perm permission of destination space's member on origin node
   * @return      return false if have issue
   */
  public String publicDocumentToSpace(String space, Node node, String comment, String perm);
  //public abstract Node getNodeByPath(String repoName,String nodePath);

}
