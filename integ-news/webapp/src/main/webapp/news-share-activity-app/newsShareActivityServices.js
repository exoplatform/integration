import {newsConstants} from '../js/newsConstants.js';

export function findUserSpaces(spaceName){
  return fetch(`${newsConstants.SOCIAL_SPACES_API}suggest.${newsConstants.format}?conditionToSearch=${spaceName}&currentUser=${newsConstants.userName}&typeOfRelation=${newsConstants.typeOfRelation}`,{
    headers:{
      'Content-Type': 'application/json'
    },
    method: 'GET'
  }).then(resp =>  resp.json()).then(json => json.options);
}