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
package org.exoplatform.wcm.ext.component.activity.listener;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Dec 8, 2014  
 */
public class Utils {

  public static ExoSocialActivity postFileActivity(Node link,
                                                   String string,
                                                   boolean b,
                                                   boolean c,
                                                   String comment) {
    ExoSocialActivity result = new ExoSocialActivityImpl();
    result.setTemplateParams(new HashMap<String, String>());
    return result;
  }

  public static ExoSocialActivity postActivity(Node link,
                                               String string,
                                               boolean b,
                                               boolean c,
                                               String comment) {
    ExoSocialActivity result = new ExoSocialActivityImpl();
    result.setTemplateParams(new HashMap<String, String>());
    return result;
  }

}
