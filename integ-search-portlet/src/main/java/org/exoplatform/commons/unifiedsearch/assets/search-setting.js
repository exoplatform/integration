
function initSearchSetting() {
  jQuery.noConflict();
  (function($){
    var CONNECTORS;

    function updateTxtSearchIn() {
      var searchIn = [];
      if($(":checkbox[name='searchInOption'][value='all']").is(":checked")) {
        $("#txtSearchIn").val("Everything");
      } else {
        $.each($(":checkbox[name='searchInOption'][value!='all']:checked"), function(){
          searchIn.push(CONNECTORS[this.value].displayName);
        });
        $("#txtSearchIn").val(searchIn.join(", "));
      }
    }


    function getSelectedTypes() {
      var searchIn = [];
      if($(":checkbox[name='searchInOption'][value='all']").is(":checked")) {
        return "all";
      } else {
        $.each($(":checkbox[name='searchInOption'][value!='all']:checked"), function(){
          searchIn.push(this.value);
        });
        return searchIn.join(",");
      }
    }


    $("#btnSave").click(function(){
      var jqxhr = $.post("/rest/search/setting", {
        resultsPerPage:$("#resultsPerPage").val(),
        searchTypes:getSelectedTypes(),
        searchCurrentSiteOnly:$("#searchCurrentSiteOnly").is(":checked"),
        hideSearchForm:$("#hideSearchForm").is(":checked"),
        hideFacetsFilter:$("#hideFacetsFilter").is(":checked")
      });

      jqxhr.complete(function(data) {
        alert("ok"==data.responseText?"Your setting has been saved.":"Problem occurred when saving your setting: "+data.responseText);
      });
    });


    $("#txtSearchIn").click(function(){
      $("#lstSearchInOptions").toggle();
      if($("#lstSearchInOptions").is(":visible")) {
        $("#lstSearchInOptions").width($("#txtSearchIn").width());
        $(":checkbox[name='searchInOption'][value!='all']").css("margin-left", "25px");
      }
    });


    $(":checkbox[name='searchInOption']").live("click", function(){
      if("all"==this.value){ //Everything checked
        if($(this).is(":checked")) { // check/uncheck all
          $(":checkbox[name='searchInOption']").attr('checked', true);
        } else {
          $(":checkbox[name='searchInOption']").attr('checked', false);
        }
      } else {
        $(":checkbox[name='searchInOption'][value='all']").attr('checked', false); //uncheck All Sites
      }

      updateTxtSearchIn();
    });


    var options = [5,10,20,50,100];
    $("#resultsPerPage").html("");
    for(var i=0; i<options.length; i++) {
      $("#resultsPerPage").append("<option>"+options[i]+"</option>");
    }

    $.getJSON("/rest/search/registry", function(registry){
      CONNECTORS = registry[0];
      var searchInOpts=[];
      searchInOpts.push("<li><label><input type='checkbox' name='searchInOption' value='all'>Everything</label></li>");
      $.each(registry[1], function(i, type){
        if(CONNECTORS[type]) searchInOpts.push("<li><label><input type='checkbox' name='searchInOption' value='" + type + "'>" + CONNECTORS[type].displayName + "</label></li>");
      });
      $("#lstSearchInOptions").html(searchInOpts.join(""));

      $.getJSON("/rest/search/setting", function(setting){
        $(":checkbox[name='searchInOption']").attr('checked', false);
        $.each($(":checkbox[name='searchInOption']"), function(){
          if(-1 != $.inArray(this.value, setting.searchTypes)) {
            $(this).attr('checked', true);
          }
        });
        $("#resultsPerPage").val(setting.resultsPerPage);
        updateTxtSearchIn();
        $("#searchCurrentSiteOnly").attr('checked', setting.searchCurrentSiteOnly);
        $("#hideSearchForm").attr('checked', setting.hideSearchForm);
        $("#hideFacetsFilter").attr('checked', setting.hideFacetsFilter);
      });

    });
  })(jQuery);
}
