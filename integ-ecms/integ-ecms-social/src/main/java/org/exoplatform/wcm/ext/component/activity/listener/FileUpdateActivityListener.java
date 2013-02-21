/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.ext.component.activity.listener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.services.cms.impl.CmsServiceImpl;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.wcm.core.NodetypeConstant;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

import javax.jcr.Value;

import org.exoplatform.services.cms.CmsService;
import org.exoplatform.services.cms.JcrInputProperty;


/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 15, 2011
 */
public class FileUpdateActivityListener extends Listener<Node, String> {

  private String[]  editedField     = {"exo:title", "exo:summary", "exo:language", "dc:title", "dc:description", "dc:creator", "dc:source", "jcr:data"};
  private String[]  bundleMessage   = {"SocialIntegration.messages.editName",
                                       "SocialIntegration.messages.editSummary",
                                       "SocialIntegration.messages.editLanguage",
                                       "SocialIntegration.messages.editTitle",
                                       "SocialIntegration.messages.editDescription",
                                       "SocialIntegration.messages.singleCreator",
                                       "SocialIntegration.messages.singleSource",
                                       "SocialIntegration.messages.editFile",
                                       "SocialIntegration.messages.editContent"};
  private String[]  bundleRemoveMessage = {"SocialIntegration.messages.removeName",
      																 	   "SocialIntegration.messages.removeSummary",
      																 	  "SocialIntegration.messages.removeLanguage",
                                           "SocialIntegration.messages.removeTitle",
                                           "SocialIntegration.messages.removeDescription",
                                           "SocialIntegration.messages.removeCreator",
                                           "SocialIntegration.messages.removeSource",
                                           "SocialIntegration.messages.editFile",
                                           "SocialIntegration.messages.removeContent"};
  
  private boolean[] needUpdate      = {true, true, false, true, true, false, false, false};
  private int consideredFieldCount = editedField.length;
  /**
   * Instantiates a new post edit content event listener.
   */
  public FileUpdateActivityListener() {
	  
  }

  @Override
  public void onEvent(Event<Node, String> event) throws Exception {
  	CmsService cmsService = WCMCoreUtils.getService(CmsService.class);
  	Map<String, Object> properties = cmsService.getPreProperties(); 
  	Map<String, Object> updatedProperties = cmsService.getUpdatedProperties();
    Node currentNode = event.getSource();
    String nodeUUID = "";
    if(currentNode.isNodeType(NodetypeConstant.MIX_REFERENCEABLE)) nodeUUID = currentNode.getUUID();
    String propertyName = event.getData();
    String oldValue = "";
    String newValue = "";
    String commentValue = "";
    try {      
    	if(currentNode.getProperty(propertyName).getDefinition().isMultiple()){
    		Value[] values = currentNode.getProperty(propertyName).getValues();
    		if(values != null && values.length > 0) {
    			for (Value value : values) {
						newValue += value.getString() + ActivityCommonService.METADATA_VALUE_SEPERATOR;
						commentValue += value.getString() + ", ";
					}
    			if(newValue.length() >= ActivityCommonService.METADATA_VALUE_SEPERATOR.length()) 
    				newValue = newValue.substring(0, newValue.length() - ActivityCommonService.METADATA_VALUE_SEPERATOR.length());
    			if(commentValue.length() >=2) commentValue = commentValue.substring(0, commentValue.length() - 2);
    		}
    		values = (Value[]) properties.get(nodeUUID + "_" + propertyName);
    		if(values != null && values.length > 0) {
    			for (Value value : values) {
    				oldValue += value.getString() + ActivityCommonService.METADATA_VALUE_SEPERATOR;
					}
    			if(oldValue.length() >= ActivityCommonService.METADATA_VALUE_SEPERATOR.length()) 
    				oldValue = oldValue.substring(0, oldValue.length() - ActivityCommonService.METADATA_VALUE_SEPERATOR.length());
    		}
    	} else {
    		newValue= currentNode.getProperty(propertyName).getString();
    		commentValue = newValue;
    		if(properties.containsKey(nodeUUID + "_" + propertyName)) 
    			oldValue = properties.get(nodeUUID + "_" + propertyName).toString();
    	}
    }catch (Exception e) {
    }
    newValue = newValue.trim();
    oldValue = oldValue.trim();
    commentValue = commentValue.trim();
    
    if(currentNode.isNodeType(NodetypeConstant.NT_RESOURCE)) currentNode = currentNode.getParent();
    String resourceBundle = "";
    boolean hit = false;
    for (int i=0; i< consideredFieldCount; i++) {
      if (propertyName.equals(editedField[i])) {
      	hit = true;
      	if(newValue.length() > 0) {
      		
      		resourceBundle = bundleMessage[i];
      		//Post activity when update dc:creator property
      		if(propertyName.equals(NodetypeConstant.DC_CREATOR))
      		{
      			List<String> lstOld = Arrays.asList(oldValue.split(ActivityCommonService.METADATA_VALUE_SEPERATOR));
    				List<String> lstNew = Arrays.asList(newValue.split(ActivityCommonService.METADATA_VALUE_SEPERATOR));
    				String itemsRemoved = "";
    				StringBuffer sb = new StringBuffer();
    				for (String item : lstOld) {
							if(!lstNew.contains(item)) sb.append(item).append(", ");
						}
    				if(sb.length() > 0) {
    				  itemsRemoved = sb.toString();
    				  itemsRemoved = itemsRemoved.substring(0, itemsRemoved.length()-2);
    				}
    				sb.delete(0, sb.length());
    				String itemsAdded = "";
    				for (String item : lstNew) {
							if(!lstOld.contains(item)) sb.append(item).append(", ");
						}
    				if(sb.length() > 0) {
    					itemsAdded = sb.toString();
    					itemsAdded = itemsAdded.substring(0, itemsAdded.length()-2);
    				}
    				
    				if(itemsRemoved.length() > 0 && itemsAdded.length() > 0){  					
    					Utils.postFileActivity(currentNode, "SocialIntegration.messages.removeCreator", needUpdate[i], true, itemsRemoved);
    					if(newValue.split(ActivityCommonService.METADATA_VALUE_SEPERATOR).length > 1)
    					  Utils.postFileActivity(currentNode, "SocialIntegration.messages.multiCreator", needUpdate[i], true, commentValue);
    					else 
    						Utils.postFileActivity(currentNode, "SocialIntegration.messages.singleCreator", needUpdate[i], true, commentValue);
    	        break;
    				}      				  
    				else if(itemsRemoved.length() > 0) {
    					resourceBundle = "SocialIntegration.messages.removeCreator";
    					newValue = itemsRemoved;
    					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, newValue);
    	        break;
    				}
    				else if(itemsAdded.length() > 0) {
    					if(newValue.split(ActivityCommonService.METADATA_VALUE_SEPERATOR).length > 1)
    					  resourceBundle = "SocialIntegration.messages.multiCreator";
    					else
    						resourceBundle = "SocialIntegration.messages.singleCreator";
    					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, commentValue);
    	        break;
    				}     			
      		}
      	  //Post activity when update dc:source property
      		if(propertyName.equals(NodetypeConstant.DC_SOURCE)) {      			
      				List<String> lstOld = Arrays.asList(oldValue.split(ActivityCommonService.METADATA_VALUE_SEPERATOR));
      				List<String> lstNew = Arrays.asList(newValue.split(ActivityCommonService.METADATA_VALUE_SEPERATOR));
      				String itemsRemoved = "";
      				StringBuffer sb = new StringBuffer();
      				for (String item : lstOld) {
								if(!lstNew.contains(item)) sb.append(item).append(", ");
							}
      				if(sb.length() > 0) {
      				  itemsRemoved = sb.toString();
      				  itemsRemoved = itemsRemoved.substring(0, itemsRemoved.length()-2);
      				}
      				sb.delete(0, sb.length());
      				String itemsAdded = "";
      				for (String item : lstNew) {
								if(!lstOld.contains(item)) sb.append(item).append(", ");
							}
      				if(sb.length() > 0) {
      					itemsAdded = sb.toString();
      					itemsAdded = itemsAdded.substring(0, itemsAdded.length()-2);
      				}
      				if(itemsRemoved.length() > 0 && itemsAdded.length() > 0){  					
      					Utils.postFileActivity(currentNode, "SocialIntegration.messages.removeSource", needUpdate[i], true, itemsRemoved);
      					Utils.postFileActivity(currentNode, "SocialIntegration.messages.addSource", needUpdate[i], true, itemsAdded);
      	        break;
      				}      				  
      				else if(itemsRemoved.length() > 0) {
      					resourceBundle = "SocialIntegration.messages.removeSource";
      					newValue = itemsRemoved;
      					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, newValue);
      	        break;
      				}
      				else if(itemsAdded.length() > 0) {
      					resourceBundle = "SocialIntegration.messages.addSource";
      					newValue = itemsAdded;
      					Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, newValue);
      	        break;
      				}      			
      		}
      		Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, commentValue);
	        break;
      	} else if(!propertyName.equals(NodetypeConstant.EXO_LANGUAGE)){ //Remove the property
      		resourceBundle = bundleRemoveMessage[i];
      		if(propertyName.equals(NodetypeConstant.DC_SOURCE) || propertyName.equals(NodetypeConstant.DC_CREATOR)) {
      			commentValue = oldValue.replaceAll(ActivityCommonService.METADATA_VALUE_SEPERATOR, ", ");
      		}
      		Utils.postFileActivity(currentNode, resourceBundle, needUpdate[i], true, commentValue);
          break;
      	} else break;
      	        
      }
    }
    if(!hit && propertyName.startsWith("dc:") && !propertyName.equals("dc:date")) {
    	if(newValue.length() > 0) {
    		resourceBundle = "SocialIntegration.messages.updateMetadata";
    		commentValue = propertyName + ActivityCommonService.METADATA_VALUE_SEPERATOR + commentValue;
    	}	else {
    		resourceBundle = "SocialIntegration.messages.removeMetadata";
    		commentValue = propertyName;
    	}
    	Utils.postFileActivity(currentNode, resourceBundle, false, true, commentValue);
    }
  }
}
