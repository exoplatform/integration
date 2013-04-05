
// Function to be called when the quick search template is ready
function initQuickSearch(portletId,seeAllMsg, noResultMsg, searching) {
  jQuery.noConflict();

  (function($){
    //*** Global variables ***
    var CONNECTORS; //all registered SearchService connectors
    var SEARCH_TYPES; //enabled search types
    var QUICKSEARCH_SETTING; //quick search setting

    var txtQuickSearchQuery_id = "#adminkeyword-" + portletId;
    var linkQuickSearchQuery_id = "#adminSearchLink-" + portletId;
    var quickSearchResult_id = "#quickSearchResult-" + portletId;
    var seeAll_id = "#seeAll-" + portletId;
    var isAlt = false;
    var value = $(txtQuickSearchQuery_id).val();
    var isDefault = false;

    var QUICKSEARCH_RESULT_TEMPLATE= " \
      <div class='quickSearchResult %{type}'> \
        <span class='avatar'> \
          %{avatar} \
        </span> \
       	<a href='%{url}' class='name'>%{title}</a> \
      </div> \
    ";//<div class='Excerpt Ellipsis'>%{excerpt}</div> \

    var QUICKSEARCH_TABLE_TEMPLATE=" \
          <table class='uiGrid table  table-hover table-striped  rounded-corners'> \
            <col width='30%'> \
            <col width='70%'> \
            %{resultRows} \
            %{messageRow} \
          </table> \
        ";

    var QUICKSEARCH_TABLE_ROW_TEMPLATE=" \
          <tr> \
            <th> \
              %{type} \
            </th> \
            <td> \
              %{results} \
            </td> \
          </tr> \
        ";

    var QUICKSEARCH_SEE_ALL=" \
        <tr> \
          <td colspan='2' class='message'> \
            <a id='seeAll-" + portletId + "' class='' href='#'>"+seeAllMsg+"</a> \
          </td> \
        </tr> \
        ";

    var QUICKSEARCH_NO_RESULT=" \
        <tr> \
          <td colspan='2' class='noResult'> \
            <span id='seeAll-" + portletId + "' class='' href='#'>"+noResultMsg+" <strong>%{query}<strong></span> \
          </td> \
        </tr> \
        ";

    var IMAGE_AVATAR_TEMPLATE = " \
      <span class='avatar pull-left'> \
        <img src='%{imageSrc}'> \
      </span> \
    ";

    var CSS_AVATAR_TEMPLATE = " \
      <span class='avatar pull-left'> \
        <i class='%{cssClass}'></i> \
      </span> \
    ";

    var EVENT_AVATAR_TEMPLATE = " \
      <div class='calendarBox calendarBox-mini'> \
        <div class='heading'> %{month} </div> \
        <div class='content' style='margin-left: 0px;'> %{date} </div> \
      </div> \
    ";

    var TASK_AVATAR_TEMPLATE = " \
      <i class='uiIconStatus-20-%{taskStatus}'></i> \
    ";  
    
    var QUICKSEARCH_WAITING_TEMPLATE=" \
        <table class='uiGrid table  table-hover table-striped  rounded-corners'> \
          <col width='30%'> \
          <col width='70%'> \
	        <tr> \
	          <td colspan='2' class='noResult'> \
	            <span id='seeAll-" + portletId + "' class='' href='#'>"+searching+" </span> \
	          </td> \
	        </tr> \
        </table> \
      ";    

    //*** Utility functions ***
    // Highlight the specified text in a string
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

    function setWaitingStatus(status) {
    	if (status){        		    	
            $(quickSearchResult_id).html(QUICKSEARCH_WAITING_TEMPLATE);
            var width = Math.min($(quickSearchResult_id).width(), $(window).width() - $(txtQuickSearchQuery_id).offset().left - 20);
            $(quickSearchResult_id).width(width);
            $(quickSearchResult_id).show();
    	}
    }

    function quickSearch() {
      var query = $(txtQuickSearchQuery_id).val();
      setWaitingStatus(true);
      var types = QUICKSEARCH_SETTING.searchTypes.join(","); //search for the types specified in quick search setting only

      var searchParams = {
        searchContext: {
          siteName:parent.eXo.env.portal.portalName
        },
        q: query,
        sites: QUICKSEARCH_SETTING.searchCurrentSiteOnly ? parent.eXo.env.portal.portalName : "all",
        types: types,
        offset: 0,
        limit: QUICKSEARCH_SETTING.resultsPerPage,
        sort: "relevancy",
        order: "desc"
      };

      // get results of all search types in a map
      $.getJSON("/rest/search", searchParams, function(resultMap){
        var rows = []; //one row per type
        $.each(SEARCH_TYPES, function(i, searchType){
          var results = resultMap[searchType]; //get all results of this type
          if(results && 0!=$(results).size()) { //show the type with result only
            results.map(function(result){result.type = searchType;}); //assign type for each result
            var cell = []; //the cell contains results of this type (in the quick search result table)
            $.each(results, function(i, result){
              cell.push(renderQuickSearchResult(result)); //add this result to the cell
            });
            var row = QUICKSEARCH_TABLE_ROW_TEMPLATE.replace(/%{type}/g, CONNECTORS[searchType].displayName).replace(/%{results}/g, cell.join(""));
            rows.push(row);
          }
        });
        setWaitingStatus(false);
        var messageRow = rows.length==0 ? QUICKSEARCH_NO_RESULT.replace(/%{query}/, query) : QUICKSEARCH_SEE_ALL;
        $(quickSearchResult_id).html(QUICKSEARCH_TABLE_TEMPLATE.replace(/%{resultRows}/, rows.join("")).replace(/%{messageRow}/g, messageRow));
        var width = Math.min($(quickSearchResult_id).width(), $(window).width() - $(txtQuickSearchQuery_id).offset().left - 20);
        $(quickSearchResult_id).width(width);
        $(quickSearchResult_id).show();
        var searchPage = "/portal/"+parent.eXo.env.portal.portalName+"/search";
        $(seeAll_id).attr("href", searchPage +"?q="+query+"&types="+types); //the query to be passed to main search page
      });
    }

    

    function renderQuickSearchResult(result) {
      var query = $(txtQuickSearchQuery_id).val();
      var terms = query.split(/\s+/g); //for highlighting
      var avatar = "";

      switch(result.type) {
        case "event":
          var date = new Date(result.fromDateTime).toString().split(/\s+/g);
          avatar = EVENT_AVATAR_TEMPLATE.
            replace(/%{month}/g, date[1]).
            replace(/%{date}/g, date[2]);
          break;

        case "task":
          avatar = TASK_AVATAR_TEMPLATE.replace(/%{taskStatus}/g, result.taskStatus);
          break;

        case "file":
        case "document":
        case "page":
          var cssClasses = $.map(result.fileType.split(/\s+/g), function(type){return "uiIcon24x24" + type}).join(" ");
          avatar = CSS_AVATAR_TEMPLATE.replace(/%{cssClass}/g, cssClasses);
          break;

        case "post":
          avatar = CSS_AVATAR_TEMPLATE.replace(/%{cssClass}/g, "uiIconUIForms");
          break;

        case "answer":
          avatar = CSS_AVATAR_TEMPLATE.replace(/%{cssClass}/g, "uiIconSocAnswersMini");
          break;

        default:
          avatar = IMAGE_AVATAR_TEMPLATE.replace(/%{imageSrc}/g, result.imageUrl);
      }


      var html = QUICKSEARCH_RESULT_TEMPLATE.
        replace(/%{type}/g, result.type).
        replace(/%{url}/g, result.url).
        replace(/%{title}/g, (result.title||"").highlight(terms)).
        replace(/%{excerpt}/g, (result.excerpt||"").highlight(terms)).
        replace(/%{detail}/g, (result.detail||"").highlight(terms)).
        replace(/%{avatar}/g, avatar);

      return html;
    }


    //*** Event handlers - Quick search ***

    $(seeAll_id).live("click", function(){
      window.location.href = $(this).attr("href"); //open the main search page
      $(quickSearchResult_id).hide();
    });


    $(txtQuickSearchQuery_id).keyup(function(e){
      if(""==$(this).val()) {
        $(quickSearchResult_id).hide();
        return;
      }
      if(13==e.keyCode) {
        $(seeAll_id).click(); //go to main search page if Enter is pressed
      } else {
        quickSearch(); //search for the text just being typed in
      }
    });

    // set th boolean variable isAlt to false  when Alt is released
    document.onkeyup = function (e) {
      if (e.which == 18) isAlt = false;
    }

    // show the input search field and place the control in it if Alt + Space are pressed
    $(document).keydown(function (e) {
      if (e.which == 18) isAlt = true;
      if (isAlt == true && e.which == 32) {
        $(txtQuickSearchQuery_id).show();
        $(txtQuickSearchQuery_id).focus();
        return false;
      }
    });

    //show the input search or go to the main search page when search link is clicked
    $(linkQuickSearchQuery_id).click(function () {
      if ($(txtQuickSearchQuery_id).is(':hidden')) {
        $(txtQuickSearchQuery_id).val(value);
        $(txtQuickSearchQuery_id).css('color', '#555');
        isDefault = true;
        $(txtQuickSearchQuery_id).show();
      }
      else
      if (isDefault == true) {
          $(txtQuickSearchQuery_id).hide();
      }
      else
        $(seeAll_id).click(); //go to main search page if Enter is pressed
    });

    $(txtQuickSearchQuery_id).focus(function(){
      $(this).val('');
      $(this).css('color', '#000');
      isDefault = false;
    });


    $(txtQuickSearchQuery_id).blur(function(){
      setTimeout(function(){$(quickSearchResult_id).hide();}, 200);
    });

    //collapse the input search field when clicking outside the search box
    $('body').click(function (evt) {
      if ($(evt.target).parents('#ToolBarSearch').length == 0) {
        $(txtQuickSearchQuery_id).hide();
      }
    });

    //*** The entry point ***
    // Load all needed configurations and settings from the service to prepare for the search
    getRegistry(function(registry){
      CONNECTORS = registry[0];
      SEARCH_TYPES = registry[1];

      getQuicksearchSetting(function(setting){
        QUICKSEARCH_SETTING = setting;
      });

    });
  })(jQuery);

  $ = jQuery; //undo .conflict();
}
