import './components/initComponents.js';
import { newsConstants } from '../js/newsConstants.js';

// getting language of the PLF
const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';

// should expose the locale resources as REST API
const url = `${newsConstants.PORTAL}/${newsConstants.PORTAL_REST}/i18n/bundle/locale.portlet.news.NewsShareActivity-${lang}.json`;

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('NewsShareActivity');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

let newsShareActivity;
// getting locale resources
export function init(params) {
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    const newsTitle = params.newsTitle.replace(/'/g, '&#39;');
    // init Vue app when locale resources are ready
    newsShareActivity = new Vue({
      el: '#newsShareActivity',
      template: `<exo-news-share-activity activityId='${params.activityId}' newsId='${params.newsId}' newsTitle='${newsTitle}'></exo-news-share-activity>`,
      i18n
    });
  });
}

export function destroy() {
  if(newsShareActivity) {
    newsShareActivity.$destroy();
  }
}