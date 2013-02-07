
function initQuickSearch() {
  //*** Global variables ***
  var CONNECTORS;
  var SEARCH_TYPES;
  var QUICKSEARCH_SETTING;
  var QUICKSEARCH_LIMIT;

  var QUICKSEARCH_RESULT_TEMPLATE= " \
    <div class='QuickSearchResult %{type}'> \
      <div class='Avatar Clickable'> \
        %{avatar} \
      </div> \
      <div class='Content'> \
        <div class='Title Ellipsis'><a href='%{url}'>%{title}</a></div> \
        <div class='Excerpt Ellipsis'>%{excerpt}</div> \
      </div> \
    </div> \
  ";

  var QUICKSEARCH_TABLE_TEMPLATE=" \
        <table style='table-layout: fixed;'> \
          <col width='30%'> \
          <col width='70%'> \
          %{resultRows} \
          <tr> \
            <th colspan='2' style='padding: 10px; font-weight: normal;'> \
              <a id='seeAll' class='Clickable' href='#'>%{message}</a> \
            </th> \
          </tr> \
        </table> \
      ";

  var QUICKSEARCH_TABLE_ROW_TEMPLATE=" \
        <tr> \
          <th> \
            <div style='margin-top: 8px; color: gray;'>%{type}</div> \
          </th> \
          <td style='padding: 2px;'> \
            <div style='margin-left: 3px;'>%{results}</div> \
          </td> \
        </tr> \
      ";


  //*** Utility functions ***

  String.prototype.highlight = function(words) {
    var str = this;
    for(var i=0; i<words.length; i++) {
      if(""==words[i]) continue;
      var regex = new RegExp("(" + words[i] + ")", "gi");
      str = str.replace(regex, "<strong>$1</strong>");
    }
    return str;
  };


  function getRegistry(callback) {
    $.getJSON("/rest/search/registry", function(registry){
      if(callback) callback(registry);
    });
  }


  function getQuicksearchSetting(callback) {
    $.getJSON("/rest/search/setting/quicksearch", function(setting){
      if(callback) callback(setting);
    });
  }


  function quickSearch() {
    var query = $("#txtQuickSearchQuery").val();
    var sites = QUICKSEARCH_SETTING.searchCurrentSiteOnly ? parent.eXo.env.portal.portalName : "all";
    var types = QUICKSEARCH_SETTING.searchTypes.join(",");
    var restUrl = "/rest/search?q=" + query+"&sites="+sites+"&types="+types+"&offset=0"+"&limit="+QUICKSEARCH_LIMIT+"&sort="+"&order=desc";

    $.getJSON(restUrl, function(resultMap){
      var rows = [];
      $.each(SEARCH_TYPES, function(i, searchType){
        results = resultMap[searchType];
        if(results && 0!=$(results).size()) {
          results.map(function(result){result.type = searchType;});
          var cell = [];
          $.each(results, function(i, result){
            cell.push(renderQuickSearchResult(result));
          });
          var row = QUICKSEARCH_TABLE_ROW_TEMPLATE.replace(/%{type}/g, CONNECTORS[searchType].displayName).replace(/%{results}/g, cell.join(""));
          rows.push(row);
        }
      });
      var message = rows.length==0 ? "No result for <strong>" + query + "<strong>" : "See All Search Results";
      $("#quickSearchResult").html(QUICKSEARCH_TABLE_TEMPLATE.replace(/%{resultRows}/, rows.join("")).replace(/%{message}/g, message));
      var width = Math.min($("#quickSearchResult").width(), $(window).width() - $("#txtQuickSearchQuery").offset().left - 20);
      $("#quickSearchResult").width(width);
      $("#quickSearchResult").show();
      var searchPage = "/portal/"+parent.eXo.env.portal.portalName+"/search";
      $("#seeAll").attr("href", searchPage +"?q="+query+"&types="+types);
    });
  }


  function renderQuickSearchResult(result) {
    var query = $("#txtQuickSearchQuery").val();
    var terms = query.split(/\\s+/g);

    var avatar = "<img src='"+result.imageUrl+"' alt='"+ result.imageUrl+"'>";

    if("event"==result.type || "task"==result.type) {
      result.url = "/portal/intranet/calendar" + result.url;
    }

    if("event"==result.type) {
      var date = new Date(result.fromDateTime).toUTCString().split(/\\s+/g);
      avatar = " \
        <div class='calendarBox'> \
          <div class='heading' style='font-size: 8px; padding: 0px; border-width: 0px; height: 10px;'>" + date[2] + "</div> \
          <div class='content' style='font-size: 10px; padding: 0px 2px; border-top-width: 0px; height: 13px;'>" + date[1] + "</div> \
        </div> \
      ";
    }

    var html = QUICKSEARCH_RESULT_TEMPLATE.
      replace(/%{type}/g, result.type).
      replace(/%{url}/g, result.url).
      replace(/%{title}/g, result.title.highlight(terms)).
      replace(/%{excerpt}/g, result.excerpt.highlight(terms)).
      replace(/%{detail}/g, result.detail.highlight(terms)).
      replace(/%{avatar}/g, avatar);

    return html;
  }


  //*** Event handlers - Quick search ***

  $("#seeAll").live("click", function(){
    window.location.href = $(this).attr("href");
    $("#quickSearchResult").hide();
  });


  $("#txtQuickSearchQuery").keyup(function(e){
    if(""==$(this).val()) {
      $("#quickSearchResult").hide();
      return;
    }

    if(13==e.keyCode) {
      $("#seeAll").click();
    } else {
      quickSearch();
    }
  });


  $("#txtQuickSearchQuery").focus(function(){
    if(""!=$("#txtQuickSearchQuery").val()) {
      quickSearch();
    }
  });


  $("#txtQuickSearchQuery").blur(function(){
    setTimeout(function(){$("#quickSearchResult").hide();}, 200);
  });


  //*** The entry point ***
  getRegistry(function(registry){
    CONNECTORS = registry[0];
    SEARCH_TYPES = registry[1];

    getQuicksearchSetting(function(setting){
      QUICKSEARCH_SETTING = setting;
      QUICKSEARCH_LIMIT = setting.resultsPerPage;
    });

  });

}
