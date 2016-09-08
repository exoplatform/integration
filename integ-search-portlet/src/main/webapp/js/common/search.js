(function($){
  
window.initSearch = function initSearch() {

    //*** Global variables ***
    var CONNECTORS; //all registered SearchService connectors
    var SEARCH_TYPES; //enabled search types
    var SEARCH_SETTING; //search setting
    var SERVER_OFFSET = 0;
    var LIMIT, RESULT_CACHE, CACHE_OFFSET, NUM_RESULTS_RENDERED;
    var formLoading;
    var searchCount = 0;

    var SEARCH_RESULT_TEMPLATE = " \
      <div class=\"resultBox clearfix %{type}\"> \
        %{avatar} \
        <div class=\"content\"> \
          <h6><a href=\"%{url}\">%{title}</a>%{rating}</h6> \
          %{space} \
          <div class=\"detail\" style='margin:10px'>%{detail}</div> \
          <p class=\"excerpt\">%{excerpt}</p> \
        </div> \
      </div> \
    ";

    var IMAGE_AVATAR_TEMPLATE = " \
      <span class=\"avatar pull-left %{userThumbnail}\"> \
        <img src=\"%{imageSrc}\" onerror=\"onImgError(this, '%{errorClasses}')\"> \
      </span> \
    ";
    
    var CSS_AVATAR_TEMPLATE = " \
      <span class=\"avatar pull-left\"> \
        <i class=\"%{cssClass}\"'></i> \
      </span> \
    ";

    var DOC_IMAGE_AVATAR_TEMPLATE = " \
      <span class=\"avatar pull-left %{userThumbnail}\"> \
        <img src=\"%{imageSrc}\" onerror=\"onImgError(this, '%{errorClasses}')\"> \
        <button class=\"btn btn-large btn-block doc-preview-thumbnail-footer\" type=\"button\">Preview</button> \
      </span> \
    ";

    var DOC_CSS_AVATAR_TEMPLATE = " \
      <span class=\"avatar pull-left\"> \
        <i class=\"%{cssClass}\"'></i> \
        <button class=\"btn btn-large btn-block doc-preview-thumbnail-footer\" type=\"button\">Preview</button> \
      </span> \
    ";

    var EVENT_AVATAR_TEMPLATE = " \
      <div class=\"avatar pull-left\"> \
        <div class=\"calendarBox\"> \
          <div class=\"heading\"> %{month} </div> \
          <div class=\"content\" style=\"margin-left: 0px;\"> %{date} </div> \
        </div> \
      </div> \
    ";

    var TASK_AVATAR_TEMPLATE = " \
      <span class=\"avatar pull-left\"> \
        <i class=\"uiIconApp64x64Task%{taskStatus}\"></i> \
      </span> \
    ";
    
    var TASK_IN_TASKS_AVATAR_TEMPLATE = " \
      <span class=\"avatar pull-left\"> \
        <i class=\"uiIcon40x40TickGray %{done}\"></i> \
      </span> \
    ";
    
    var TASK_IN_TASKS_DETAIL_TEMPLATE = " \
      <a href=\"#\"> \
        <i class=\"uiIconFolder taskProjectIconSearchDetail\"></i> %{projectName} \
      </a> \
      <i class=\"uiIconColorPriority%{priority} taskPriorityIconSearchDetail\"></i>\
      <span>%{dueDate}</span>\
    ";

    var RATING_TEMPLATE = " \
      <div class=\"uiVote pull-right\"> \
        <div class=\"avgRatingImages clearfix\"> \
          %{rating} \
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
        if("" == words[i]) continue;
        var regex;
        if(isSpecialExpressionCharacter(words[i].charAt(0))) {
          regex = new RegExp("(\\" + words[i] + ")", "gi");
        } else {
          regex = new RegExp("(" + words[i] + ")", "gi");
        }
        str = str.replace(regex, "<strong>$1</strong>");
      }
      return str;
    };

    // Remove all HTML tags except <strong>, </strong> (for highlighting)
    String.prototype.escapeHtml = function() {
      return this.
        replace(/<(\/?strong)>/g,"{\$1}"). //save <strong>, </strong> as {strong}, {/strong}
        replace(/<.+?>/g,"").              //remove all HTML tags
        replace(/{(\/?strong)}/g,"<\$1>"); //restore {strong}, {/strong} back to <strong>, </strong>
    }

    function isSpecialExpressionCharacter(c) {
        var specials = '`~!@#$%^&*()-=+{}[]\|;:\'"<>,./?';
        for(var i = 0; i < specials.length; i++) {
            if(c == specials.charAt(i)) {
                return true;
            }
        }
        return false;
    }

    function setWaitingStatus(status) {
      if(status) {
    	$("#resultLoading").show();
    	var w = $("#resultLoading").css('width').replace("px","");
    	var h = $("#resultLoading").css('height').replace("px","");
    	var left = (document.width/2)-(w/2);
    	var top = (document.height/2)-(h/2);
    	
    	$('#resultLoading').css('left',left + 200);
    	$('#resultLoading').css('top',top - 50);
      } else {    	  
	    $("#resultLoading").hide();
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
          var key =eXo.ecm.WCMUtils.getBundle("unifiedSearch.type." + connector.displayName , eXo.env.portal.language);
          contentTypes.push("<li><span class='uiCheckbox'><input type='checkbox' class='checkbox' name='contentType' value='" + connector.searchType + "'><span></span></span>" + key + "</li>");

        }
      });
      if(0!=contentTypes.length) {
        $("#lstContentTypes").html(contentTypes.join(""));
      } else {
        // Disable All Content Types checkbox if there's no content type to show
        $(":checkbox[name='contentType'][value='all']").attr("disabled", "disabled");
      }
    }

    function loadSiteFilter(callback) {
      $.getJSON("/rest/search/sites", function(sites){
        var siteNames = [];
        $.each(sites, function(i, site){
          siteNames.push("<li><span class='uiCheckbox'><input type='checkbox' class='checkbox' name='site' value='" + site + "'><span></span></span>" + site.toProperCase() + "</li>");
        });
        if(0!=siteNames.length) {
          $("#lstSites").html(siteNames.join(""));
        } else {
          // Disable All Sites checkbox if there's no site to show
          $(":checkbox[name='site'][value='all']").attr("disabled", "disabled");
        }
        if(callback) callback(sites); //pass the available sites to be used in the callback function
      });
    }

    function getSelectedTypes(){
      var selectedTypes = [];
      $.each($(":checkbox[name='contentType'][value!='all']:checked"), function(){
        selectedTypes.push(this.value);
      });
      return selectedTypes.join(",");
    }

    function getSelectedSites(){
      if(SEARCH_SETTING.searchCurrentSiteOnly) return getUrlParam("currentSite") || eXo.env.portal.portalName;
      var selectedSites = [];
      $.each($(":checkbox[name='site'][value!='all']:checked"), function(){
        selectedSites.push(this.value);
      });
      return selectedSites.join(",");
    }
    
    function getSpaceName(nodePath) {
      var groupPrefix = "/Groups/spaces/";
      if (!nodePath.startsWith(groupPrefix)) {
        return false;
      }
      var path = nodePath.substring(groupPrefix.length);
      return path.substring(0, path.indexOf("/"));
    }
    
    function getFolderName(nodePath) {
      var i = nodePath.lastIndexOf("/");
      return nodePath.substring(i + 1); 
    }

    function renderSearchResult(result) {
      var query = $("#txtQuery").val();
      var terms = query.split(/\s+/g);
      var avatar = "";
      var rating = "";
      var count = searchCount ++;
      var space = "<div id='space_place_holder_" + count +"'/>";

      switch(result.type) {
        case "event":
          var d = new Date(result.fromDateTime);
          // Convert fromDateTime to user calendar timezone
          var fromDateTime = d.getTime() + d.getTimezoneOffset() * 60 * 1000 + result.timeZoneOffset;
          var date = new Date(fromDateTime).toString().split(/\s+/g);
          avatar = EVENT_AVATAR_TEMPLATE.
            replace(/%{month}/g, date[1]).
            replace(/%{date}/g, date[2]);
          break;

        case "task":
          var taskStatus = result.taskStatus;
          var taskStatusClass = taskStatus === "needs-action" ? "NeedActions" : (taskStatus === "in-process" ? "Progress" : (taskStatus === "canceled" ? "Canceled" : "Done"));
          avatar = TASK_AVATAR_TEMPLATE.replace(/%{taskStatus}/g, taskStatusClass);
          break;
        case "tasksInTasks":
          var projectName = result.projectName ? result.projectName : '';
          var priority = result.priority ? result.priority : '';
          var dueDate = result.dueDate ? result.dueDate : '';
          
          var detail = TASK_IN_TASKS_DETAIL_TEMPLATE.replace(/%{projectName}/g, projectName);
          detail = detail.replace(/%{priority}/g, priority);
          detail = detail.replace(/%{dueDate}/g, dueDate);
          result.detail = detail;
          //
          var doneClass = result.completed ? 'uiIcon40x40TickBlue' : '';
          avatar = TASK_IN_TASKS_AVATAR_TEMPLATE.replace(/%{done}/g, doneClass);
          break;
        case "file":
            var cssClasses = $.map(result.fileType.split(/\s+/g), function(type){return "uiIcon64x64" + type}).join(" ");
            if (result.imageUrl == null || result.imageUrl == ""){
            	avatar = DOC_CSS_AVATAR_TEMPLATE.replace(/%{cssClass}/g, cssClasses);
            }else{
                avatar = DOC_IMAGE_AVATAR_TEMPLATE.replace(/%{imageSrc}/g, result.imageUrl).replace(/%{errorClasses}/g, cssClasses).replace(/%{userThumbnail}/g, "");
            }
            var previewUrl = result.previewUrl;
            if(previewUrl == null) {
              previewUrl = result.url;
            }
            avatar = "<a href=\""+previewUrl+"\">" + avatar + "</a>";
            
            break;
        case "document":
          var cssClasses = $.map(result.fileType.split(/\s+/g), function(type){return "uiIcon64x64Template" + type}).join(" ");
          avatar = DOC_CSS_AVATAR_TEMPLATE.replace(/%{cssClass}/g, cssClasses);
          var previewUrl = result.previewUrl;
          if(previewUrl == null) {
            previewUrl = result.url;
          }
          avatar = "<a href=\""+previewUrl+"\">" + avatar + "</a>";
          break;

        case "page":
    	  result.detail = result.detail + " - " + result.url;
          avatar = IMAGE_AVATAR_TEMPLATE.replace(/%{imageSrc}/g, result.imageUrl).replace(/%{userThumbnail}/g, "");
    	  result.excerpt = result.excerpt;
          break;
          
        case "post":
        case "answer":
          //render rating
          var voted = Math.floor(result.rating);
          var remainder = result.rating - voted;
          var votedHalf = (0.3<remainder && remainder<0.7) ? 1 : 0;
          if(remainder>0.7) voted++;
          var unvoted = 5 - voted - votedHalf;

          rating = Array(voted+1).join("<i class='voted'></i>");
          rating += Array(votedHalf+1).join("<i class='votedHaft'></i>");
          rating += Array(unvoted+1).join("<i class='unvoted'></i>");

          rating = RATING_TEMPLATE.replace(/%{rating}/g, rating);

          avatar = CSS_AVATAR_TEMPLATE.replace(/%{cssClass}/g, result.type === "post" ? "uiIconApp64x64Forum" : "uiIconApp64x64Answers");
          break;
        case "wiki":
            avatar = CSS_AVATAR_TEMPLATE.replace(/%{cssClass}/g, "uiIconApp64x64Wiki");
          break;
        default:
          avatar = IMAGE_AVATAR_TEMPLATE.replace(/%{imageSrc}/g, result.imageUrl);
          if (result.type === "space" || result.type === "people") {
            avatar = avatar.replace(/%{userThumbnail}/g, "userThumbnail");
          }
      }

      var html = SEARCH_RESULT_TEMPLATE.
        replace(/%{type}/g, result.type).
        replace(/%{url}/g, result.url).
        replace(/%{title}/g, (result.title||"").highlight(terms)).
        replace(/%{excerpt}/g, (result.excerpt||"").escapeHtml().highlight(terms)).
        replace(/%{detail}/g, (result.detail||"").highlight(terms)).
        replace(/%{avatar}/g, avatar).
        replace(/%{rating}/g, rating).
        replace(/%{space}/g, space);

      $("#result").append(html);
      if (result.type == "file") {
          var spaceName = getSpaceName(result.nodePath);
          if (spaceName) {
            $.ajax({
              dataType: "json",
              url: "/" + eXo.env.portal.rest + "/private/" + eXo.env.portal.containerName + "/social/spaces/spaceInfo/?spaceName=" + spaceName
            }).done(function(data) {
              var sName = data.displayName;
              var imgSrc = data.imageSource;
              var sUri = data.url;
              spaceHtml =  "<div>" +
                             "<a class='spaceName' rel='tooltip' data-placement='bottom' title='" + sName + "' href='" + sUri + "' style='color:black'>" +
                               "<img title='' alt='' src='" + imgSrc + "' class='spaceIcon avatarMini' /><strong>&nbsp;" + sName + "</strong>" +
                             "</a>" +
                             "<div id='file_path_place_holder" + count + "' style='display:inline'></div>" + 
                           "</div>";
              $("#space_place_holder_" + count).html(spaceHtml);
              $.ajax({
                dataType: "json",
                url: "/" + eXo.env.portal.rest + "/document/docOpenUri?nodePath=" + result.nodePath  
              }).done(function(data) {
                  var nodePathsHtml = "";
                  var keys = [];
                  $.each(data, function(key, value) {
                    keys.push(key);
                  });
                  keys.sort();
                  for (var i = 1; i < keys.length - 1; i++) {
                      var key = keys[i];
                      var value = data[key];
                      nodePathsHtml += "<icon class='uiIconArrowRight' style='margin:5px'></icon>" +
                      "<a href='" + value + "' style='color:black'><strong>" + getFolderName(key) + "</strong></a>";
                  }
                  //console.log(nodePathsHtml);
                  $("#file_path_place_holder" + count).html(nodePathsHtml);
              }).fail(function () {
                  console.log("Can not get document open uri!");
              });
            }).fail(function () {
              console.log("Can not get space info!");
            });
          }
      }
    }

    function clearResultPage(){
      $("#result").html("");
      $("#resultHeader").html("");
      $("#resultSort").hide();
      $("#resultPage").hide();
      $("#resultPage").removeClass("noResult");
      setWaitingStatus(false);
      return;
    }

    function showNoResultPage(key){
      clearResultPage();
      var resultPage = $("#resultPage");
      resultPage.addClass("noResult");
      $("#keyword",resultPage).html(key);
      resultPage.show();
    }

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
      if ((a.title||"").toUpperCase() < (b.title||"").toUpperCase())
        return -1;
      if ((a.title||"").toUpperCase() > (b.title||"").toUpperCase())
        return 1;
      return 0;
    }
    function byTitleDESC(b,a) {
      if ((a.title||"").toUpperCase() < (b.title||"").toUpperCase())
        return -1;
      if ((a.title||"").toUpperCase() > (b.title||"").toUpperCase())
        return 1;
      return 0;
    }

    function search(callback) {
      SERVER_OFFSET = 0;
      NUM_RESULTS_RENDERED = 0;

      getFromServer(function(){
        renderCachedResults();
      });
    }
    function pushStateToHistory(current_results){
         var query = $("#txtQuery").val();
         var types = getUrlParam("types");
         var searchPage = window.location.pathname;
         var urlPath= searchPage + "?q="+query+"&types="+types;
         window.history.pushState({"cached_results":current_results,"query":query},"", urlPath);
     }

    function getFromServer(callback){
      var query = $("#txtQuery").val();
      if(""==query) {
        clearResultPage();
        return;
      }

      var sort = $("#sortField").attr("sort");
      var order = $("#sortField").attr("order");

      setWaitingStatus(true);
      
      var searchParams = {
        searchContext: {
          siteName:eXo.env.portal.portalName
        },
        q: query,
        sites: getSelectedSites(),
        types: getSelectedTypes(),
        offset: SERVER_OFFSET,
        limit: LIMIT,
        sort: sort,
        order: order
      };

      $.getJSON("/rest/search", searchParams, function(resultMap){
        RESULT_CACHE = [];
        $.each(resultMap, function(searchType, results){
          //results.map(function(result){result.type = searchType;});
          $.map(results, function(result){result.type = searchType;});
          RESULT_CACHE.push.apply(RESULT_CACHE, results);
        });

        //var sortFuncName = "by" + sort.toProperCase() + order.toUpperCase();
        if (order.toUpperCase() == "DESC"){
        	if (sort.toProperCase() == "Relevancy"){
                RESULT_CACHE = RESULT_CACHE.sort(function(a,b){
                	return byRelevancyDESC(a,b);	  
                }); //sort the result set
        	}else if (sort.toProperCase() == "Date"){
        		RESULT_CACHE = RESULT_CACHE.sort(function(a,b){
        		return byDateDESC(a,b);
        		});
        	}else {
        		RESULT_CACHE = RESULT_CACHE.sort(function(a,b){
        		return byTitleDESC(a,b);
        		});
        	}
        }else{
        	if (sort.toProperCase() == "Relevancy"){
        		RESULT_CACHE = RESULT_CACHE.sort(function(a,b){
        		return byRelevancyASC(a,b);
        		});
        	}else if (sort.toProperCase() == "Date"){
        		RESULT_CACHE = RESULT_CACHE.sort(function(a,b){
        		return byDateASC(a,b);
        		});
        	}else {
        		RESULT_CACHE = RESULT_CACHE.sort(function(a,b){
        		return byTitleASC(a,b);
        		});
        	}        	
        }
        
        //RESULT_CACHE = RESULT_CACHE.sort(eval(sortFuncName)); //sort the result set

        CACHE_OFFSET = 0; //reset the local offset

        if(callback) callback();
        if(RESULT_CACHE.length < LIMIT) $("#showMore").hide(); else $("#showMore").show();
        setWaitingStatus(false);
      });
    }
    
    window.onpopstate = function(e){
        if(e.state){
           renderCachedResults(null, e.state.cached_results,e.state.query);
        }
    };
    
    function renderCachedResults(append,current_results,query) {
      var current;
      if(current_results){
        current = current_results;
        $("#txtQuery").val(query);
      }else{
        current = RESULT_CACHE.slice(CACHE_OFFSET, CACHE_OFFSET+LIMIT);
        pushStateToHistory(current);
      }    
      if(0==current.length) {
        if(append) {
          $("#showMore").hide();
        } else {
        	showNoResultPage(XSSUtils.sanitizeString($("#txtQuery").val()))
        }
        return;
      }

      NUM_RESULTS_RENDERED = NUM_RESULTS_RENDERED + current.length;
      var resultHeader =eXo.ecm.WCMUtils.getBundle("unifiedSearch.label.Results" , eXo.env.portal.language).replace("{0}",1).replace("{1}",NUM_RESULTS_RENDERED).replace("{2}","<strong>" +XSSUtils.sanitizeString($("#txtQuery").val())+ "<strong>");
      $("#resultHeader").html(resultHeader);
      $("#resultSort").show();
      $("#resultPage").removeClass("noResult");
      $("#resultPage").show();      

      if(!append) $("#result").html("");
      $.each(current, function(i, result){
        renderSearchResult(result);
      });
    }

    //*** Event handlers ***
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

    $(document).on("click",":checkbox[name='contentType']", function(){
      if("all"==this.value){ //All Content Types checked
        if($(this).is(":checked")) { // check/uncheck all
          $(":checkbox[name='contentType']").attr('checked', true);
        } else {
          $(":checkbox[name='contentType']").attr('checked', false);
        }
      } else {
        $(":checkbox[name='contentType'][value='all']").attr('checked', false); //uncheck All Content Types
      }

      window.search = search(); //perform search again to update the results
    });

    $(document).on("click",":checkbox[name='site']", function(){
      if("all"==this.value){ //All Sites checked
        if($(this).is(":checked")) { // check/uncheck all
          $(":checkbox[name='site']").attr('checked', true);
        } else {
          $(":checkbox[name='site']").attr('checked', false);
        }
      } else {
        $(":checkbox[name='site'][value='all']").attr('checked', false); //uncheck All Sites
      }

      window.search = search(); //perform search again to update the results
    });

    $("#btnSearch").click(function(){
      window.search = search();
    });

    $("#txtQuery").keyup(function(e){
      var keyCode = e.keyCode || e.which;
      if(13==keyCode) window.search = search();
    });

    $("#sortOptions > li > a").on("click", function(){
      var oldOption = $("#sortField").attr("sort");
      var newOption = $(this).attr("sort");

      if(newOption==oldOption) { //click a same option again
        $(this).children("i").toggleClass("uiIconSortUp uiIconSortDown"); //toggle the arrow
      } else {
        $("#sortField").text($(this).text());
        $("#sortField").attr("sort", newOption);

        $("#sortOptions > li > a > i").remove(); //remove the arrows from other options

        // Select the default sort order: DESC for Relevancy, ASC for Date & Title
        var sortByIcon;
        switch(newOption) {
          case "relevancy":
            sortByIcon = 'uiIconSortDown';
            break;
          case "date":
            sortByIcon = 'uiIconSortUp';
            break;
          case "title":
            sortByIcon = 'uiIconSortUp';
            break;
        }

        $(this).append("<i class='" + sortByIcon + "'></i>"); //add the arrow to this option
      }

      $("#sortField").attr("order", $(this).children("i").hasClass("uiIconSortUp") ? "asc" : "desc");

      SERVER_OFFSET = 0;
      NUM_RESULTS_RENDERED = 0;
      getFromServer(function(){
        renderCachedResults();
      });

    });

    //*** The entry point ***
    getRegistry(function(registry){ //load all configuration from the registry
      CONNECTORS = registry[0];
      SEARCH_TYPES = registry[1];
      getSearchSetting(function(setting){
        SEARCH_SETTING = setting;

        // Display search page elements base on the configurations in search setting
        if(!setting.hideFacetsFilter) {
          $("#facetsFilter").show();
        }

        if(!setting.hideSearchForm) {
          $("#searchForm").show();
        }

        loadContentFilter(CONNECTORS, setting);

        if(0!=SEARCH_TYPES.join().length && 0!=SEARCH_SETTING.searchTypes.join().length) { //there're content types to show
          var types = getUrlParam("types"); //get the requested content types from url
          if(types) {
            $.each($(":checkbox[name='contentType']"), function(){
              $(this).attr('checked', -1!=types.indexOf(this.value) || -1!=types.indexOf("all")); //check the according options
            });
          } else {
            $(":checkbox[name='contentType']").attr("checked", true); //check all types by default
          }
        }

        // Render the search page's query, limit, sort, order with their values got from url (or default values)
        $("#txtQuery").val(getUrlParam("q"));
        $("#txtQuery").focus();

        var limit = getUrlParam("limit");
        LIMIT = limit && !isNaN(parseInt(limit)) ? parseInt(limit) : setting.resultsPerPage;
        var offset = getUrlParam("offset");
        SERVER_OFFSET = offset && !isNaN(parseInt(offset)) ? parseInt(offset) : SERVER_OFFSET;        
        var sort = getUrlParam("sort")||"relevancy";
        var order = getUrlParam("order") || "desc";
        $("#sortField").text($("#sortOptions > li > a[sort='" + sort + "']").text());
        $("#sortField").attr("sort", sort);
        $("#sortField").attr("order", order);
        $("#sortOptions > li > a > i").remove(); //remove the arrows from other options
        $("#sortOptions > li > a[sort='" + sort + "']").append("<i class='" + (order=="asc"?"uiIconSortUp":"uiIconSortDown") + "'></i>"); //add the arrow to this option

        if(setting.searchCurrentSiteOnly) { //search without site filter
          $("#siteFilter").hide();
          //window.search = search();
          NUM_RESULTS_RENDERED = 0;          
          getFromServer(function(){
            renderCachedResults();
          });          
          
        } else { //search with site filter
          loadSiteFilter(function(availableSites){ //show site filter
            if(0!=availableSites.join().length) { //there're sites to show
              var sites = getUrlParam("sites"); //get the requested sites from url
              if(sites) {
                $.each($(":checkbox[name='site']"), function(){
                  $(this).attr('checked', -1!=sites.indexOf(this.value) || -1!=sites.indexOf("all")); //check the according options
                });
              } else {
                $(":checkbox[name='site']").attr('checked', true);  //check all sites by default
              }
            }
            //window.search = search();
            NUM_RESULTS_RENDERED = 0;
            getFromServer(function(){
              renderCachedResults();
            });            
          });
        }

      });
    });
}


//js for search setting
window.initSearchSetting = function initSearchSetting(allMsg,alertOk,alertNotOk){

    var CONNECTORS; //all registered SearchService connectors
    var CHECKBOX_TEMPLATE = "\
      <div class='control-group'> \
        <div class='controls-full'> \
          <span class='uiCheckbox'> \
            <input type='checkbox' class='checkbox' name='%{name}' value='%{value}'> \
            <span>%{text}</span> \
          </span> \
        </div> \
      </div> \
    ";


    function getSelectedTypes() {
      var searchIn = [];
      if($(":checkbox[name='searchInOption'][value='all']").is(":checked")) {
        return "all";
      } else {
        $.each($(":checkbox[name='searchInOption'][value!='all']:checked"), function(){
          searchIn.push(this.value);
        });
        if (searchIn.length==0){
        	return "false";
        }
        return searchIn.join(",");
      }
    }


    // Call REST service to save the setting
    $("#btnSave").click(function(){
      //var url = "/rest/search/setting/"+$("#resultsPerPage").val()+"/"+getSelectedTypes()+"/"+$("#searchCurrentSiteOnly").is(":checked").toString()+"/"+$("#hideSearchForm").is(":checked").toString()+"/"+$("#hideFacetsFilter").is(":checked").toString();      
      var jqxhr = $.post("/rest/search/setting", {
        resultsPerPage: $("#resultsPerPage").val(),
        searchTypes: getSelectedTypes(),
        searchCurrentSiteOnly: $("#searchCurrentSiteOnly").is(":checked"),
        hideSearchForm: $("#hideSearchForm").is(":checked"),
        hideFacetsFilter: $("#hideFacetsFilter").is(":checked")
      });

      jqxhr.complete(function(data) {
        alert("ok"==data.responseText?"Your setting has been saved.":"Problem occurred when saving your setting: "+data.responseText);
      });
    });


    // Handler for the checkboxes
    $(":checkbox[name='searchInOption']").live("click", function(){
      if("all"==this.value){ //All checked
        if($(this).is(":checked")) { // check/uncheck all
          $(":checkbox[name='searchInOption']").attr('checked', true);
        } else {
          $(":checkbox[name='searchInOption']").attr('checked', false);
        }
      } else {
        $(":checkbox[name='searchInOption'][value='all']").attr('checked', false); //uncheck All Sites
      }
    });


    // Load all needed configurations and settings from the service to build the UI
    $.getJSON("/rest/search/registry", function(registry){
      CONNECTORS = registry[0];
      var searchInOpts=[];
      searchInOpts.push(CHECKBOX_TEMPLATE.
        replace(/%{name}/g, "searchInOption").
        replace(/%{value}/g, "all").
        replace(/%{text}/g, allMsg));
      $.each(registry[1], function(i, type){
        if(CONNECTORS[type]) searchInOpts.push(CHECKBOX_TEMPLATE.
          replace(/%{name}/g, "searchInOption").
          replace(/%{value}/g, type).
          replace(/%{text}/g, eXo.ecm.WCMUtils.getBundle("unifiedSearch.type." + CONNECTORS[type].displayName , eXo.env.portal.language)));
      });
      $("#lstSearchInOptions").html(searchInOpts.join(""));

      // Display the previously saved (or default) search setting
      $.getJSON("/rest/search/setting", function(setting){
        if(-1 != $.inArray("all", setting.searchTypes)) {
          $(":checkbox[name='searchInOption']").attr('checked', true);
        } else {
          $(":checkbox[name='searchInOption']").attr('checked', false);
          $.each($(":checkbox[name='searchInOption']"), function(){
            if(-1 != $.inArray(this.value, setting.searchTypes)) {
              $(this).attr('checked', true);
            }
          });
        }
        $("#resultsPerPage").val(setting.resultsPerPage);
        $("#searchCurrentSiteOnly").attr('checked', setting.searchCurrentSiteOnly);
        $("#hideSearchForm").attr('checked', setting.hideSearchForm);
        $("#hideFacetsFilter").attr('checked', setting.hideFacetsFilter);
      });

    });
}

/**
 * Handle error event when image cannot load in unified search
 * 
 */
window.onImgError = function onImgError(object, errorClasses) {
  object.onerror = null;
  $(object).parent().empty().append($(document.createElement('i')).addClass(errorClasses));
}

})($);