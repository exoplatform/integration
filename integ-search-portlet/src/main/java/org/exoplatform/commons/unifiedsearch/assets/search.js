
function initSearch() {
  jQuery.noConflict();

  (function($){
    //*** Global variables ***
    var CONNECTORS; //all registered SearchService connectors
    var SEARCH_TYPES; //enabled search types
    var SEARCH_SETTING; //search setting
    var SERVER_OFFSET = 0;
    var LIMIT, RESULT_CACHE, CACHE_OFFSET, NUM_RESULTS_RENDERED;
    var formLoading;

    var SEARCH_RESULT_TEMPLATE = " \
      <div class='resultBox clearfix %{type}'> \
        %{avatar} \
        <div class='content'> \
          <h6><a href='%{url}'>%{title}</a>%{rating}</h6> \
          <p class='excerpt'>%{excerpt}</p> \
          <div class='detail'>%{detail}</div> \
        </div> \
      </div> \
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
      <div class='pull-left'> \
        <div class='calendarBox'> \
          <div class='heading'> %{month} </div> \
          <div class='content' style='margin-left: 0px;'> %{date} </div> \
        </div> \
      </div> \
    ";

    var TASK_AVATAR_TEMPLATE = " \
      <div class='pull-left'> \
        <i class='uiIconStatus-64-%{taskStatus}'></i> \
      </div> \
    ";

    var RATING_TEMPLATE = " \
      <div class='uiVote pull-right'> \
        <div class='avgRatingImages clearfix'> \
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
        if(""==words[i]) continue;
        var regex = new RegExp("(" + words[i] + ")", "gi");
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


    function setWaitingStatus(status) {
      if(status) {
    	$("#resultLoading").show();
    	var w = $("#resultLoading").css('width').replace("px","");
    	var h = $("#resultLoading").css('height').replace("px","");
    	var left = (window.screen.width/2)-(w/2);
    	var top = (window.screen.height/2)-(h/2);
    	
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
          contentTypes.push("<li><span class='uiCheckbox'><input type='checkbox' class='checkbox' name='contentType' value='" + connector.searchType + "'><span></span></span>" + connector.displayName + "</li>");
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
      if(SEARCH_SETTING.searchCurrentSiteOnly) return getUrlParam("currentSite") || parent.eXo.env.portal.portalName;
      var selectedSites = [];
      $.each($(":checkbox[name='site'][value!='all']:checked"), function(){
        selectedSites.push(this.value);
      });
      return selectedSites.join(",");
    }


    function renderSearchResult(result) {
      var query = $("#txtQuery").val();
      var terms = query.split(/\s+/g);
      var avatar = "";
      var rating = "";

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
            var cssClasses = $.map(result.fileType.split(/\s+/g), function(type){return "uiIcon64x64" + type}).join(" ");
            if (result.imageUrl == null || result.imageUrl == ""){
            	avatar = CSS_AVATAR_TEMPLATE.replace(/%{cssClass}/g, cssClasses);
            }else{
            	avatar = IMAGE_AVATAR_TEMPLATE.replace(/%{imageSrc}/g, result.imageUrl);
            }
            avatar = "<a href='"+result.url+"'>" + avatar + "</a>";            
            break;        	        	
        case "document":
        //case "page":
          var cssClasses = $.map(result.fileType.split(/\s+/g), function(type){return "uiIcon64x64" + type}).join(" ");
          avatar = CSS_AVATAR_TEMPLATE.replace(/%{cssClass}/g, cssClasses);
          break;

        case "page":
    	  result.detail = result.detail + " - " + result.url;
    	  avatar = IMAGE_AVATAR_TEMPLATE.replace(/%{imageSrc}/g, result.imageUrl);
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

          if("post"==result.type) {
            avatar = CSS_AVATAR_TEMPLATE.replace(/%{cssClass}/g, "uiIconAppForumPortlet");
          } else {
            avatar = CSS_AVATAR_TEMPLATE.replace(/%{cssClass}/g, "uiIconAppAnswersPortlet");
          }
          break;

        default:
          avatar = IMAGE_AVATAR_TEMPLATE.replace(/%{imageSrc}/g, result.imageUrl);
      }

      var html = SEARCH_RESULT_TEMPLATE.
        replace(/%{type}/g, result.type).
        replace(/%{url}/g, result.url).
        replace(/%{title}/g, (result.title||"").highlight(terms)).
        replace(/%{excerpt}/g, (result.excerpt||"").escapeHtml().highlight(terms)).
        replace(/%{detail}/g, (result.detail||"").highlight(terms)).
        replace(/%{avatar}/g, avatar).
        replace(/%{rating}/g, rating);

      $("#result").append(html);
    }


    function clearResultPage(message){
      $("#result").html("");
      $("#resultHeader").html(message?message:"");
      $("#resultSort").hide();
      $("#resultPage").hide();
      setWaitingStatus(false);
      return;
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
          siteName:parent.eXo.env.portal.portalName
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

        var sortFuncName = "by" + sort.toProperCase() + order.toUpperCase();
        RESULT_CACHE = RESULT_CACHE.sort(eval(sortFuncName)); //sort the result set

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
          clearResultPage("No result for <strong>" + $("#txtQuery").val() + "<strong>");
        }
        return;
      }

      NUM_RESULTS_RENDERED = NUM_RESULTS_RENDERED + current.length;
      var resultHeader = "Results " + 1 + " to " + NUM_RESULTS_RENDERED + " for <strong>" + $("#txtQuery").val() + "<strong>";
      $("#resultHeader").html(resultHeader);
      $("#resultSort").show();
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

      search(); //perform search again to update the results
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

      search(); //perform search again to update the results
    });


    $("#btnSearch").click(function(){
      search();
    });


    $("#txtQuery").keyup(function(e){
      var keyCode = e.keyCode || e.which;
      if(13==keyCode) search();
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
          //search();
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
            //search();
            NUM_RESULTS_RENDERED = 0;
            getFromServer(function(){
              renderCachedResults();
            });            
          });
        }

      });
    });

  })(jQuery);

  $ = jQuery; //undo .conflict();
}