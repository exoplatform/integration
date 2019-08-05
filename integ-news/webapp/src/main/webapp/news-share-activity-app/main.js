import './components/initComponents.js';
import { newsConstants } from '../js/newsConstants.js';

// getting language of the PLF
const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';

// should expose the locale ressources as REST API
const url = `${newsConstants.PORTAL}/${newsConstants.PORTAL_REST}/i18n/bundle/locale.portlet.news.NewsShareActivity-${lang}.json`;

// get overrided components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('NewsShareActivity');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

let newsShareActivity;
// getting locale ressources
export function init() {
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale ressources are ready
    newsShareActivity = new Vue({
      el: '#newsShareActivity',
      template: '<exo-news-share-activity></exo-news-share-activity>',
      i18n
    });
  });
}

export function destroy() {
  if(newsShareActivity) {
    newsShareActivity.$destroy();
  }
}