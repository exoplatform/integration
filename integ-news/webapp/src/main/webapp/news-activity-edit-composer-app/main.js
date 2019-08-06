import './components/initComponents.js';
import { newsConstants } from '../js/newsConstants.js';

// getting language of the PLF
const lang = typeof eXo !== 'undefined' ? eXo.env.portal.language : 'en';

// should expose the locale resources as REST API
const url = `${newsConstants.PORTAL}/${newsConstants.PORTAL_REST}/i18n/bundle/locale.portlet.news.NewsActivityComposer-${lang}.json`;

// get overridden components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('NewsActivityEditComposer');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

let newsActivityUpdateApp;
// getting locale resources
export function init(params) {
  exoi18n.loadLanguageAsync(lang, url).then(i18n => {
    // init Vue app when locale resources are ready
    newsActivityUpdateApp = new Vue({
      el: '#newsActivityEditComposer',
      template: `<exo-news-activity-edit-composer activityId=${params.activityId}></exo-news-activity-edit-composer>`,
      i18n
    });
  });
}

export function destroy() {
  if(newsActivityUpdateApp) {
    newsActivityUpdateApp.$destroy();
  }
}