
function Search(registry, setting) {
  var _this = this;
  this.connectors = registry[0];
  this.searchTypes = registry[1];
  this.setting = setting;

  //var search; //the main search function

  this.VIEWING_TYPE = undefined;
  this.RESULT_CACHE = [];
  this.CACHE_OFFSET = 0;
  this.SERVER_OFFSET = 0;
  this.NUM_RESULTS_RENDERED = 0;

  this.loadContentFilter();
  this.loadSiteFilter(function(){
    var sites = Search.getUrlParam("sites");
    if(sites) {
      $.each($(":checkbox[name='site']"), function(){
        $(this).attr('checked', -1!=sites.indexOf(this.value) || -1!=sites.indexOf("all"));
      });
    } else {
      $(":checkbox[name='site']").attr('checked', true);  //check all sites by default
    }

    if(query && !setting.searchCurrentSiteOnly) Search.search();
  });

  if(setting.hideFacetsFilter) {
    $("#facetsFilter").hide();
  }

  if(setting.hideSearchForm) {
    $("#searchForm").hide();
  } else {
    $("#txtQuery").focus();
  }


  var query = Search.getUrlParam("q");
  $("#txtQuery").val(query);

  var types = Search.getUrlParam("types");
  if(types) {
    $.each($(":checkbox[name='contentType']"), function(){
      $(this).attr('checked', -1!=types.indexOf(this.value) || -1!=types.indexOf("all"));
    });
  } else {
    $(":checkbox[name='contentType']").attr('checked', true); //check all types by default
  }

  $("#lstSortBy").val(Search.getUrlParam("sort")||"relevancy");
  var order = Search.getUrlParam("order");
  $("#sortType").removeClass("Asc Desc").addClass(order && order.toUpperCase()=="ASC" ? "Asc" : "Desc");

  var limit = Search.getUrlParam("limit");
  this.limit = limit && !isNaN(parseInt(limit)) ? parseInt(limit) : setting.resultsPerPage;

  $("#txtQuery").focus();

  if(query && setting.searchCurrentSiteOnly) Search.search();

}


Search.RESULT_TEMPLATE = " \
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


Search.setWaitingStatus = function(status) {
  if(status) {
    $("body").css("cursor", "wait");
    $("#searchPortlet").css({"pointer-events":"none"});
  } else {
    $("body").css("cursor", "auto");
    $("#searchPortlet").css({"pointer-events":"auto"});
  }
}


Search.getUrlParam = function(name) {
  var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
  return match && decodeURIComponent(match[1].replace(/\\+/g, ' '));
}


Search.prototype.loadContentFilter = function() {
  var _this = this;
  var contentTypes = [];
  $.each(_this.searchTypes, function(i, searchType){
    var connector = _this.connectors[searchType];
    // Show only the types user selected in setting
    if(connector && (-1 != $.inArray("all", _this.setting.searchTypes) || -1 != $.inArray(searchType, _this.setting.searchTypes))) {
      contentTypes.push("<li><label><input type='checkbox' name='contentType' value='" + connector.searchType + "'>" + connector.displayName + "</label></li>");
    }
  });
  if(0!=contentTypes.length) {
    $("#lstContentTypes").html(contentTypes.join(""));
  } else {
    $(":checkbox[name='contentType'][value='all']").attr("checked", false).attr("disabled", "disabled");
  }
}


Search.prototype.loadSiteFilter = function(callback) {
  if(this.setting.searchCurrentSiteOnly) {
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


Search.getSelectedTypes = function(){
  var selectedTypes = [];
  $.each($(":checkbox[name='contentType'][value!='all']:checked"), function(){
    selectedTypes.push(this.value);
  });
  return selectedTypes.join(",");
}


Search.prototype.getSelectedSites = function() {
  if(this.setting.searchCurrentSiteOnly) return Search.getUrlParam("currentSite") || parent.eXo.env.portal.portalName;
  var selectedSites = [];
  $.each($(":checkbox[name='site'][value!='all']:checked"), function(){
    selectedSites.push(this.value);
  });
  return selectedSites.join(",");
}


Search.prototype.categorizedSearch = function(callback) {
  var _this = this;
  var query = $("#txtQuery").val();
  var sql = $("#txtSql").val().replace(/%query%/g, query);
  if(!Search.isSqlMode() && ""==query) {
    Search.clearResultPage();
    return;
  }

  var sort = $("#lstSortBy").val();
  var order = $("#sortType").hasClass("Asc") ? "asc" : "desc";

  var restUrl = "/rest/search?q=" + (Search.isSqlMode()?sql:query+"&sites="+this.getSelectedSites()+"&types="+Search.getSelectedTypes()+"&offset=0"+"&limit="+_this.limit+"&sort="+sort+"&order="+order);

  Search.setWaitingStatus(true);

  $.getJSON(restUrl, function(resultMap){
    var resultTypesHtml = "| ";
    var resultHtml = "";

    $.each(_this.searchTypes, function(i, searchType){
      var results = resultMap[searchType];
      if(results) results.map(function(result){result.type = searchType;});
      if(0!=$(results).size()) {
        var typeDisplayName = Search.isSqlMode() ? searchType + " (" + $(results).size() + ")" : _this.connectors[searchType].displayName;
        resultTypesHtml = resultTypesHtml + "<span class='Clickable ResultType' type='" + searchType + "' offset=0 numEntries=" + $(results).size() + ">" + typeDisplayName + "</span>" + " | ";
        resultHtml = resultHtml + "<div class='SearchResultType' id='" + searchType + "-type'>";

        $.each(results, function(i, result){
          resultHtml = resultHtml + Search.renderSearchResult(result);
        });
        resultHtml = resultHtml + "</div>"; //type div
      }
    });

    if(""==resultHtml) {
      Search.clearResultPage("No result for <strong>" + (Search.isSqlMode()?sql:$("#txtQuery").val()) + "<strong>");
      return;
    }

    $("#resultTypes").html(resultTypesHtml+"<hr style='color: lightgray'/>");
    $("#resultTypes").show();

    $("#result").html(resultHtml);
    $("#result").show();
    $("#resultSort").show();
    Search.setWaitingStatus(false);

    if(callback) callback();
    (_this.VIEWING_TYPE && $(".ResultType[type='" + _this.VIEWING_TYPE + "']").get(0) ? $(".ResultType[type='" + _this.VIEWING_TYPE + "']").get(0) : $(".ResultType").first()).click();
  });
}


Search.renderSearchResult = function(result) {
  var template = Search.RESULT_TEMPLATE;
  var query = $("#txtQuery").val();
  var terms = query.split(/\\s+/g);

  var avatar = "<img src='"+result.imageUrl+"' alt='"+ result.imageUrl+"'>";

  if("event"==result.type || "task"==result.type) {
    result.url = "/portal/intranet/calendar" + result.url;
  }

  if("event"==result.type) {
    var date = new Date(result.date).toUTCString().split(/\\s+/g);
    avatar = " \
      <div class='calendarBox'> \
        <div class='heading' style='padding-top: 0px; padding-bottom: 0px; border-width: 0px;'>" + date[2] + "</div> \
        <div class='content' style='padding: 0px 6px; padding-bottom: 0px; border-top-width: 0px;'>" + date[1] + "</div> \
      </div> \
    ";
  }

  var html = template.
    replace(/%{type}/g, result.type).
    replace(/%{url}/g, result.url).
    replace(/%{title}/g, result.title.highlight(terms)).
    replace(/%{excerpt}/g, result.excerpt.highlight(terms)).
    replace(/%{detail}/g, result.detail.highlight(terms)).
    replace(/%{avatar}/g, avatar);

  if(IS_CATEGORIZED) return html;
  $("#result").append(html);
}


Search.clearResultPage = function(message){
  $("#resultTypes").html("");
  $("#result").html("");
  $("#resultHeader").html(message?message:"");
  $("#resultSort").hide();
  $("#showMore").hide();
  Search.setWaitingStatus(false);
  return;
}


Search.prototype.showMore = function(offsetIncrement) {
  var _this = this;
  if(Search.isSqlMode()) return;
  var query = $("#txtQuery").val();
  if(""==query) {
    Search.clearResultPage();
    return;
  }

  var $viewingType = $(".ResultType.Selected");
  var type = $viewingType.attr("type");
  var offset = offsetIncrement!=0 ? parseInt($viewingType.attr("offset"))+offsetIncrement : 0; //if sorting then start from begining
  var sortBy = $("#lstSortBy").val();
  var sortType = $("#sortType").hasClass("Asc") ? "asc" : "desc";

  var restUrl = "/rest/search?q="+query+"&sites="+this.getSelectedSites()+"&types="+type+"&offset="+offset+"&limit="+_this.limit+"&sort="+sortBy+"&order="+sortType;

  Search.setWaitingStatus(true);

  $.getJSON(restUrl, function(resultMap){
    var resultHtml = "";
    var results = resultMap[type];
    results.map(function(result){result.type = type;});
    $.each(results, function(i, result){
      resultHtml = resultHtml + Search.renderSearchResult(result);
    });

    var resultHeader = (0==results.length) ? "No more result" : "Results 1 to " + (offset+results.length);
    resultHeader = resultHeader + " for <strong>" + (Search.isSqlMode()?$("#txtSql").val().replace(/%query%/g, $("#txtQuery").val()):$("#txtQuery").val()) + "<strong>";
    $("#resultHeader").html(resultHeader);
    if(results.length==_this.limit) $("#showMore").show(); else $("#showMore").hide();

    $("#"+type+"-type").show();

    if(0==offsetIncrement) {
      $("#"+type+"-type").html(resultHtml);
    } else {
      $("#"+type+"-type").append(resultHtml);
      $("#searchPage").animate({ scrollTop: $("#resultPage")[0].scrollHeight}, "slow");
    }

    Search.setWaitingStatus(false);

    $viewingType.attr("offset", offset);
    $viewingType.attr("numEntries", results.length);
  });
}


Search.isSqlMode = function() {
  return $("#sqlExec").is(":visible");
}


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


Search.prototype.uncategorizedSearch = function(callback) {
  var _this = this;
  _this.SERVER_OFFSET = 0;
  _this.NUM_RESULTS_RENDERED = 0;

  this.getFromServer(function(){
    _this.renderCachedResults();
  });
}


Search.prototype.getFromServer = function(callback) {
  var _this = this;
  var query = $("#txtQuery").val();
  var sql = $("#txtSql").val().replace(/%query%/g, query);
  if(!Search.isSqlMode() && ""==query) {
    Search.clearResultPage();
    return;
  }

  var sort = $("#lstSortBy").val();
  var order = $("#sortType").hasClass("Asc") ? "asc" : "desc";

  Search.setWaitingStatus(true);

  var restUrl = "/rest/search?q="+ (Search.isSqlMode() ? sql : query+"&sites="+_this.getSelectedSites()+"&types="+Search.getSelectedTypes()+"&offset="+_this.SERVER_OFFSET+"&limit="+_this.limit+"&sort="+sort+"&order="+order);
  $.getJSON(restUrl, function(resultMap){
    _this.RESULT_CACHE = [];
    $.each(resultMap, function(searchType, results){
      results.map(function(result){result.type = searchType;});
      _this.RESULT_CACHE.push.apply(_this.RESULT_CACHE, results);
    });

    var sortFuncName = "by" + sort.toProperCase() + order.toUpperCase();
    _this.RESULT_CACHE = _this.RESULT_CACHE.sort(window[sortFuncName]); //sort the result set

    _this.CACHE_OFFSET = 0; //reset the local offset

    if(callback) callback();
    if(_this.RESULT_CACHE.length < _this.limit) $("#showMore").hide(); else $("#showMore").show();
    Search.setWaitingStatus(false);
  });
}


Search.prototype.renderCachedResults = function(append) {
  var _this = this;
  var current = _this.RESULT_CACHE.slice(_this.CACHE_OFFSET, _this.CACHE_OFFSET+this.limit);
  if(0==current.length) {
    Search.clearResultPage("No result for <strong>" + (Search.isSqlMode()?$("#txtSql").val().replace(/%query%/g, $("#txtQuery").val()):$("#txtQuery").val()) + "<strong>");
    return;
  }

  _this.NUM_RESULTS_RENDERED = _this.NUM_RESULTS_RENDERED + current.length;
  var resultHeader = "Results " + 1 + " to " + _this.NUM_RESULTS_RENDERED + " for <strong>" + (Search.isSqlMode()?$("#txtSql").val().replace(/%query%/g, $("#txtQuery").val()):$("#txtQuery").val()) + "<strong>";
  $("#resultHeader").html(resultHeader);
  $("#resultSort").show();

  if(!append) $("#result").html("");
  $.each(current, function(i, result){
    Search.renderSearchResult(result);
  });
}



