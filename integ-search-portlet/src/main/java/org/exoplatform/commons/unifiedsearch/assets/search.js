
function initSearch() {
  //*** Global variables ***
  var CONNECTORS;
  var SEARCH_TYPES;
  var SEARCH_SETTING;
  var LIMIT;
  var VIEWING_TYPE;

  var RESULT_CACHE, CACHE_OFFSET, SERVER_OFFSET, NUM_RESULTS_RENDERED; //for uncategoried search

  var IS_CATEGORIZED = false;

  var search; //the main search function

  var SEARCH_RESULT_TEMPLATE = " \
    <div class='SearchResult %{type}'> \
      <div class='Avatar Clickable'> \
        %{avatar} \
      </div> \
      <div class='Content'> \
        <div class='Title Ellipsis'><a href='%{url}'>%{title}</a></div> \
        <div class='Excerpt Ellipsis'>%{excerpt}</div> \
        <div class='Detail'>%{detail}</div> \
      </div> \
    </div> \
  ";


  //*** Utility functions ***

  String.prototype.toProperCase = function() {
    return this.replace(/\w\S*/g, function(txt){return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();});
  };


  String.prototype.highlight = function(words) {
    var str = this;
    for(var i=0; i<words.length; i++) {
      if(""==words[i]) continue;
      var regex = new RegExp("(" + words[i] + ")", "gi");
      str = str.replace(regex, "<strong>$1</strong>");
    }
    return str;
  };


  function setWaitingStatus(status) {
    if(status) {
      $("body").css("cursor", "wait");
      $("#searchPortlet").css({"pointer-events":"none"});
    } else {
      $("body").css("cursor", "auto");
      $("#searchPortlet").css({"pointer-events":"auto"});
    }
  }


  function getUrlParam(name) {
    var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
    return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
  }


  function getRegistry(callback) {
    $.getJSON("/rest/search/registry", function(registry){
      if(callback) callback(registry);
    });
  }


  function getSearchSetting(callback) {
    $.getJSON("/rest/search/setting", function(setting){
      if(callback) callback(setting);
    });
  }


  function loadContentFilter(connectors, searchSetting) {
    var contentTypes = [];
    $.each(SEARCH_TYPES, function(i, searchType){
      var connector = connectors[searchType];
      // Show only the types user selected in setting
      if(connector && (-1 != $.inArray("all", searchSetting.searchTypes) || -1 != $.inArray(searchType, searchSetting.searchTypes))) {
        contentTypes.push("<li><label><input type='checkbox' name='contentType' value='" + connector.searchType + "'>" + connector.displayName + "</label></li>");
      }
    });
    if(0!=contentTypes.length) {
      $("#lstContentTypes").html(contentTypes.join(""));
    } else {
      $(":checkbox[name='contentType'][value='all']").attr("checked", false).attr("disabled", "disabled");
    }
  }


  function loadSiteFilter(searchSetting, callback) {
    if(searchSetting.searchCurrentSiteOnly) {
      $("#siteFilter").hide();
    } else {
      $.getJSON("/rest/search/sites", function(sites){
        var siteNames = [];
        $.each(sites, function(i, site){
          siteNames.push("<li><label><input type='checkbox' name='site' value='" + site + "'>" + site.toProperCase() + "</label></li>");
        });
        $("#lstSites").html(siteNames.join(""));
        if(callback) callback();
      });
    }
  }


  function getSelectedTypes(){
    var selectedTypes = [];
    $.each($(":checkbox[name='contentType'][value!='all']:checked"), function(){
      selectedTypes.push(this.value);
    });
    return selectedTypes.join(",");
  }


  function getSelectedSites(){
    if(SEARCH_SETTING.searchCurrentSiteOnly) return getUrlParam("currentSite") || parent.eXo.env.portal.portalName;
    var selectedSites = [];
    $.each($(":checkbox[name='site'][value!='all']:checked"), function(){
      selectedSites.push(this.value);
    });
    return selectedSites.join(",");
  }


  function categorizedSearch(callback) {
    var query = $("#txtQuery").val();
    var sql = $("#txtSql").val().replace(/%query%/g, query);
    if(!isSqlMode() && ""==query) {
      clearResultPage();
      return;
    }

    var sort = $("#lstSortBy").val();
    var order = $("#sortType").hasClass("Asc") ? "asc" : "desc";

    var restUrl = "/rest/search?q=" + (isSqlMode()?sql:query+"&sites="+getSelectedSites()+"&types="+getSelectedTypes()+"&offset=0"+"&limit="+LIMIT+"&sort="+sort+"&order="+order);

    setWaitingStatus(true);

    $.getJSON(restUrl, function(resultMap){
      var resultTypesHtml = "| ";
      var resultHtml = "";

      $.each(SEARCH_TYPES, function(i, searchType){
        var results = resultMap[searchType];
        if(results) results.map(function(result){result.type = searchType;});
        if(0!=$(results).size()) {
          var typeDisplayName = isSqlMode() ? searchType + " (" + $(results).size() + ")" : CONNECTORS[searchType].displayName;
          resultTypesHtml = resultTypesHtml + "<span class='Clickable ResultType' type='" + searchType + "' offset=0 numEntries=" + $(results).size() + ">" + typeDisplayName + "</span>" + " | ";
          resultHtml = resultHtml + "<div class='SearchResultType' id='" + searchType + "-type'>";

          $.each(results, function(i, result){
            resultHtml = resultHtml + renderSearchResult(result);
          });
          resultHtml = resultHtml + "</div>"; //type div
        }
      });

      if(""==resultHtml) {
        clearResultPage("No result for <strong>" + (isSqlMode()?sql:$("#txtQuery").val()) + "<strong>");
        return;
      }

      $("#resultTypes").html(resultTypesHtml+"<hr style='color: lightgray'/>");
      $("#resultTypes").show();

      $("#result").html(resultHtml);
      $("#result").show();
      $("#resultSort").show();
      setWaitingStatus(false);

      if(callback) callback();
      (VIEWING_TYPE && $(".ResultType[type='" + VIEWING_TYPE + "']").get(0) ? $(".ResultType[type='" + VIEWING_TYPE + "']").get(0) : $(".ResultType").first()).click();
    });
  }


  function renderSearchResult(result) {
    var query = $("#txtQuery").val();
    var terms = query.split(/\s+/g);

    var avatar = "<img src='"+result.imageUrl+"' alt='"+ result.imageUrl+"'>";

    if("event"==result.type || "task"==result.type) {
      result.url = "/portal/intranet/calendar" + result.url;
    }

    if("event"==result.type) {
      var date = new Date(result.fromDateTime).toString().split(/\s+/g);
      avatar = " \
        <div class='calendarBox'> \
          <div class='heading' style='padding-top: 0px; padding-bottom: 0px; border-width: 0px;'>" + date[1] + "</div> \
          <div class='content' style='padding: 0px 6px; padding-bottom: 0px; border-top-width: 0px;'>" + date[2] + "</div> \
        </div> \
      ";
    }

    if ("task"==result.type){
      avatar = "\
        <div class='statusTask'>\
          <i class='"+result.imageUrl+"Icon'></i>\
        </div>\
      ";
    }

    var html = SEARCH_RESULT_TEMPLATE.
      replace(/%{type}/g, result.type).
      replace(/%{url}/g, result.url).
      replace(/%{title}/g, result.title.highlight(terms)).
      replace(/%{excerpt}/g, result.excerpt.highlight(terms)).
      replace(/%{detail}/g, result.detail.highlight(terms)).
      replace(/%{avatar}/g, avatar);

    if(IS_CATEGORIZED) return html;
    $("#result").append(html);
  }


  function clearResultPage(message){
    $("#resultTypes").html("");
    $("#result").html("");
    $("#resultHeader").html(message?message:"");
    $("#resultSort").hide();
    $("#showMore").hide();
    setWaitingStatus(false);
    return;
  }


  function showMore(offsetIncrement) {
    if(isSqlMode()) return;
    var query = $("#txtQuery").val();
    if(""==query) {
      clearResultPage();
      return;
    }

    var $viewingType = $(".ResultType.Selected");
    var type = $viewingType.attr("type");
    var offset = offsetIncrement!=0 ? parseInt($viewingType.attr("offset"))+offsetIncrement : 0; //if sorting then start from begining
    var sortBy = $("#lstSortBy").val();
    var sortType = $("#sortType").hasClass("Asc") ? "asc" : "desc";

    var restUrl = "/rest/search?q="+query+"&sites="+getSelectedSites()+"&types="+type+"&offset="+offset+"&limit="+LIMIT+"&sort="+sortBy+"&order="+sortType;

    setWaitingStatus(true);

    $.getJSON(restUrl, function(resultMap){
      var resultHtml = "";
      var results = resultMap[type];
      results.map(function(result){result.type = type;});
      $.each(results, function(i, result){
        resultHtml = resultHtml + renderSearchResult(result);
      });

      var resultHeader = (0==results.length) ? "No more result" : "Results 1 to " + (offset+results.length);
      resultHeader = resultHeader + " for <strong>" + (isSqlMode()?$("#txtSql").val().replace(/%query%/g, $("#txtQuery").val()):$("#txtQuery").val()) + "<strong>";
      $("#resultHeader").html(resultHeader);
      if(results.length==LIMIT) $("#showMore").show(); else $("#showMore").hide();

      $("#"+type+"-type").show();

      if(0==offsetIncrement) {
        $("#"+type+"-type").html(resultHtml);
      } else {
        $("#"+type+"-type").append(resultHtml);
        $("#searchPage").animate({ scrollTop: $("#resultPage")[0].scrollHeight}, "slow");
      }

      setWaitingStatus(false);

      $viewingType.attr("offset", offset);
      $viewingType.attr("numEntries", results.length);
    });
  }


  function isSqlMode() {
    return $("#sqlExec").is(":visible");
  }


  //*** Event handlers - Search page ***

  $(".ResultType").live("click", function(){
    $(".Selected").toggleClass("Selected");
    $(this).toggleClass("Selected");

    var offset = parseInt($(this).attr("offset"));
    var numEntries = parseInt($(this).attr("numEntries"));

    var resultHeader = (0==numEntries) ? "No more result" : "Results 1 to " + (offset+numEntries);
    resultHeader = resultHeader + " for <strong>" + (isSqlMode()?$("#txtSql").val().replace(/%query%/g, $("#txtQuery").val()):$("#txtQuery").val()) + "<strong>";
    $("#resultHeader").html(resultHeader);
    if(numEntries==LIMIT) $("#showMore").show(); else $("#showMore").hide();

    var type=$(this).attr("type");
    $(".SearchResultType").hide(); //hide all other types
    $("#"+type+"-type").show();

    var $viewingType = $(".ResultType.Selected");
    $("#lstSortBy").val($viewingType.attr("sortBy")||"relevancy").attr("selected",true);
    $("#sortType").removeClass("Asc Desc").addClass($viewingType.attr("sortType")||"Desc");
  });


  $(".SearchResult .Avatar img").live("click", function(){
    var url = $(this).parents("div.SearchResult").find(".Content > .Title > a").attr("href");

    //get jcr node properties
    if(0 == url.indexOf("/rest/jcr/")) {
      $.getJSON("/rest/search/jcr/props?node=" + url.replace("/rest/jcr/", ""), function(props){
        var sProps = "";
        $.each(props, function(key, value){
          sProps = sProps + key + " = " + value + "\n";
        });
        console.log(props);
        alert(sProps);
      });
    }
  });


  $(":checkbox[name='contentType']").live("click", function(){
    if("all"==this.value){ //All Content Types checked
      if($(this).is(":checked")) { // check/uncheck all
        $(":checkbox[name='contentType']").attr('checked', true);
      } else {
        $(":checkbox[name='contentType']").attr('checked', false);
      }
    } else {
      $(":checkbox[name='contentType'][value='all']").attr('checked', false); //uncheck All Content Types
    }

    if(!isSqlMode()) {
      VIEWING_TYPE = $(".ResultType.Selected").attr("type"); //save the current view before performing search
      var checkedType = $(this).is(":checked") ? this.value : undefined;
      search(function(){
        if(checkedType && $(".ResultType[type='" + checkedType + "']").get(0)) VIEWING_TYPE = checkedType;
      });
    }
  });


  $(":checkbox[name='site']").live("click", function(){
    if("all"==this.value){ //All Sites checked
      if($(this).is(":checked")) { // check/uncheck all
        $(":checkbox[name='site']").attr('checked', true);
      } else {
        $(":checkbox[name='site']").attr('checked', false);
      }
    } else {
      $(":checkbox[name='site'][value='all']").attr('checked', false); //uncheck All Sites
    }

    if(!isSqlMode()) {
      VIEWING_TYPE = $(".ResultType.Selected").attr("type"); //save the current view before performing search
      search();
    }
  });


  $("#btnSearch").click(function(){
    search();
  });


  $("#txtQuery").focus(function(){
    VIEWING_TYPE = $(".ResultType.Selected").attr("type"); //save the current view before performing search
  });


  $("#txtQuery").keyup(function(e){
    var keyCode = e.keyCode || e.which,
        arrow = {up: 38, down: 40 };

    switch (keyCode) {
      case arrow.up:
        $("#sqlExec").hide();
        break;
      case arrow.down:
        $("#sqlExec").show();
        break;
      case 13:
        search();
    }
  });


  /*** Uncategorized search***/

  // Client-side sort functions
  function byRelevancyASC(a,b) {
    if (a.relevancy < b.relevancy)
      return -1;
    if (a.relevancy > b.relevancy)
      return 1;
    return 0;
  }
  function byRelevancyDESC(b,a) {
    if (a.relevancy < b.relevancy)
      return -1;
    if (a.relevancy > b.relevancy)
      return 1;
    return 0;
  }

  function byDateASC(a,b) {
    if (a.date < b.date)
      return -1;
    if (a.date > b.date)
      return 1;
    return 0;
  }
  function byDateDESC(b,a) {
    if (a.date < b.date)
      return -1;
    if (a.date > b.date)
      return 1;
    return 0;
  }

  function byTitleASC(a,b) {
    if (a.title < b.title)
      return -1;
    if (a.title > b.title)
      return 1;
    return 0;
  }
  function byTitleDESC(b,a) {
    if (a.title < b.title)
      return -1;
    if (a.title > b.title)
      return 1;
    return 0;
  }


  function uncategorizedSearch(callback) {
    SERVER_OFFSET = 0;
    NUM_RESULTS_RENDERED = 0;

    getFromServer(function(){
      renderCachedResults();
    });
  }


  function getFromServer(callback){
    var query = $("#txtQuery").val();
    var sql = $("#txtSql").val().replace(/%query%/g, query);
    if(!isSqlMode() && ""==query) {
      clearResultPage();
      return;
    }

    var sort = $("#lstSortBy").val();
    var order = $("#sortType").hasClass("Asc") ? "asc" : "desc";

    setWaitingStatus(true);

    var restUrl = "/rest/search?q="+ (isSqlMode() ? sql : query+"&sites="+getSelectedSites()+"&types="+getSelectedTypes()+"&offset="+SERVER_OFFSET+"&limit="+LIMIT+"&sort="+sort+"&order="+order);
    $.getJSON(restUrl, function(resultMap){
      RESULT_CACHE = [];
      $.each(resultMap, function(searchType, results){
        results.map(function(result){result.type = searchType;});
        RESULT_CACHE.push.apply(RESULT_CACHE, results);
      });

      var sortFuncName = "by" + sort.toProperCase() + order.toUpperCase();
      RESULT_CACHE = RESULT_CACHE.sort(window[sortFuncName]); //sort the result set

      CACHE_OFFSET = 0; //reset the local offset

      if(callback) callback();
      if(RESULT_CACHE.length < LIMIT) $("#showMore").hide(); else $("#showMore").show();
      setWaitingStatus(false);
    });
  }


  function renderCachedResults(append) {
    var current = RESULT_CACHE.slice(CACHE_OFFSET, CACHE_OFFSET+LIMIT);
    if(0==current.length) {
      if(append) {
        $("#showMore").hide();
      } else {
        clearResultPage("No result for <strong>" + (isSqlMode()?$("#txtSql").val().replace(/%query%/g, $("#txtQuery").val()):$("#txtQuery").val()) + "<strong>");
      }
      return;
    }

    NUM_RESULTS_RENDERED = NUM_RESULTS_RENDERED + current.length;
    var resultHeader = "Results " + 1 + " to " + NUM_RESULTS_RENDERED + " for <strong>" + (isSqlMode()?$("#txtSql").val().replace(/%query%/g, $("#txtQuery").val()):$("#txtQuery").val()) + "<strong>";
    $("#resultHeader").html(resultHeader);
    $("#resultSort").show();

    if(!append) $("#result").html("");
    $.each(current, function(i, result){
      renderSearchResult(result);
    });
  }


  if(IS_CATEGORIZED) {
    search = function(callback) {
      categorizedSearch(callback);
    };

    $("#btnShowMore").click(function(){
      showMore(LIMIT);
    });

    $("#lstSortBy").change(function(){
      $(".ResultType.Selected").attr("sortBy", $(this).val());
      showMore(0);
    });

    $("#sortType").live("click", function(){
      $(this).toggleClass("Asc Desc");
      $(".ResultType.Selected").attr("sortType", $(this).hasClass("Asc") ? "Asc" : "Desc");
      showMore(0);
    });

  } else {
    search = function(callback) {
      uncategorizedSearch(callback);
    };

    $("#btnShowMore").click(function(){
      CACHE_OFFSET = CACHE_OFFSET + LIMIT;
      var remaining = RESULT_CACHE.slice(CACHE_OFFSET, CACHE_OFFSET+LIMIT);

      if(remaining.length < LIMIT) {
        SERVER_OFFSET = SERVER_OFFSET + LIMIT;
        getFromServer(function(){
          RESULT_CACHE = remaining.concat(RESULT_CACHE);
          renderCachedResults(true);
          $("#searchPage").animate({ scrollTop: $("#resultPage")[0].scrollHeight}, "slow");
        });
        return;
      }
      renderCachedResults(true);
      $("#searchPage").animate({ scrollTop: $("#resultPage")[0].scrollHeight}, "slow");
    });

    $("#lstSortBy").change(function(){
      SERVER_OFFSET = 0;
      NUM_RESULTS_RENDERED = 0;
      getFromServer(function(){
        renderCachedResults();
      });
    });

    $("#sortType").live("click", function(){
      $(this).toggleClass("Asc Desc");
      SERVER_OFFSET = 0;
      NUM_RESULTS_RENDERED = 0;
      getFromServer(function(){
        renderCachedResults();
      });
    });

  }


  //*** The entry point ***
  getRegistry(function(registry){
    CONNECTORS = registry[0];
    SEARCH_TYPES = registry[1];
    getSearchSetting(function(setting){
      SEARCH_SETTING = setting;

      loadContentFilter(CONNECTORS, setting);
      loadSiteFilter(setting, function(){
        var sites = getUrlParam("sites");
        if(sites) {
          $.each($(":checkbox[name='site']"), function(){
            $(this).attr('checked', -1!=sites.indexOf(this.value) || -1!=sites.indexOf("all"));
          });
        } else {
          $(":checkbox[name='site']").attr('checked', true);  //check all sites by default
        }

        if(query && !setting.searchCurrentSiteOnly) search();
      });

      if(!setting.hideFacetsFilter) {
        $("#facetsFilter").show();
      }

      if(!setting.hideSearchForm) {
        $("#searchForm").show();
        $("#txtQuery").focus();
      }

      var query = getUrlParam("q");
      $("#txtQuery").val(query);

      var types = getUrlParam("types");
      if(types) {
        $.each($(":checkbox[name='contentType']"), function(){
          $(this).attr('checked', -1!=types.indexOf(this.value) || -1!=types.indexOf("all"));
        });
      } else {
        $(":checkbox[name='contentType']").attr('checked', true); //check all types by default
      }

      $("#lstSortBy").val(getUrlParam("sort")||"relevancy");
      var order = getUrlParam("order");
      $("#sortType").removeClass("Asc Desc").addClass(order && order.toUpperCase()=="ASC" ? "Asc" : "Desc");

      var limit = getUrlParam("limit");
      LIMIT = limit && !isNaN(parseInt(limit)) ? parseInt(limit) : setting.resultsPerPage;

      $("#txtQuery").focus();

      if(query && setting.searchCurrentSiteOnly) search();

    });
  });

  var sortBy = [];
  var sortFields = ["relevancy", "date", "title"];
  $.each(sortFields, function(i, field){
    sortBy.push("<option value='" + field + "'>" + field.toProperCase() + "</option>")
  });
  $("#lstSortBy").html(sortBy.join(""));

}
