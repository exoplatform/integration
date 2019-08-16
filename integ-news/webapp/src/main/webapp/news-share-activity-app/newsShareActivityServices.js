import {newsConstants} from '../js/newsConstants.js';

export function findUserSpaces(spaceName) {
  return fetch(`${newsConstants.SOCIAL_SPACES_SUGGESTION_API}?conditionToSearch=${spaceName}&currentUser=${newsConstants.userName}&typeOfRelation=confirmed`,{
    headers:{
      'Content-Type': 'application/json'
    },
    method: 'GET'
  }).then(resp =>  resp.json()).then(json => json.options);
}

export function shareNews(newsId, activityId, sharedDescription, sharedSpaces) {
  const sharedNews = {
    description: sharedDescription,
    spacesNames: sharedSpaces,
    activityId: activityId
  };
  return fetch(`${newsConstants.NEWS_API}/${newsId}/share`,{
    headers:{
      'Content-Type': 'application/json'
    },
    method: 'POST',
    body: JSON.stringify(sharedNews)
  });
}