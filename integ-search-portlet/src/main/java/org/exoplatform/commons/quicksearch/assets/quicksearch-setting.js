
// Function to be called when the quick search setting template is ready
function initQuickSearchSetting(){
  var CONNECTORS; //all registered SearchService connectors

  // update the "Search in" text box with values selected in the dropdown list
  function updateTxtSearchIn() {
    var searchIn = [];
    if($(":checkbox[name='searchInOption'][value='all']").is(":checked")) {
      $("#txtSearchIn").val("Everything"); //put Everything if all checkboxes is checked
    } else {
      $.each($(":checkbox[name='searchInOption'][value!='all']:checked"), function(){
        searchIn.push(CONNECTORS[this.value].displayName); //put in the selected checkboxes
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


  // Call REST service to save the setting
  $("#btnSave").click(function(){
    var jqxhr = $.post("/rest/search/setting/quicksearch", {
      resultsPerPage:$("#resultsPerPage").val(),
      searchTypes:getSelectedTypes(),
      searchCurrentSiteOnly:$("#searchCurrentSiteOnly").is(":checked")
    });

    jqxhr.complete(function(data) {
      alert("ok"==data.responseText?"Your setting has been saved.":"Problem occurred when saving your setting: "+data.responseText);
    });
  });


  // Show the search type list when clicking on the textbox
  $("#txtSearchIn").click(function(){
    $("#lstSearchInOptions").toggle();
    if($("#lstSearchInOptions").is(":visible")) {
      $("#lstSearchInOptions").width($("#txtSearchIn").width());
      $(":checkbox[name='searchInOption'][value!='all']").css("margin-left", "25px");
    }
  });


  // Handler for the checkboxes
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


  // Add 10 options (1..10) for "Results per page" field
  $("#resultsPerPage").html("");
  for(var i=1; i<=10; i++) {
    $("#resultsPerPage").append("<option>"+i+"</option>");
  }

  // Load all needed configurations and settings from the service to build the UI
  $.getJSON("/rest/search/registry", function(registry){
    CONNECTORS = registry[0];
    var searchInOpts=[];
    searchInOpts.push("<li><label><input type='checkbox' name='searchInOption' value='all'>Everything</label></li>");
    $.each(registry[1], function(i, type){
      if(CONNECTORS[type]) searchInOpts.push("<li><label><input type='checkbox' name='searchInOption' value='" + type + "'>" + CONNECTORS[type].displayName + "</label></li>");
    });
    $("#lstSearchInOptions").html(searchInOpts.join(""));

    // Display the previously saved (or default) quick search setting
    $.getJSON("/rest/search/setting/quicksearch", function(setting){
      $(":checkbox[name='searchInOption']").attr('checked', false);
      $.each($(":checkbox[name='searchInOption']"), function(){
        if(-1 != $.inArray(this.value, setting.searchTypes)) {
          $(this).attr('checked', true);
        }
      });

      $("#resultsPerPage").val(setting.resultsPerPage);
      updateTxtSearchIn();
      $("#searchCurrentSiteOnly").attr('checked', setting.searchCurrentSiteOnly);
    });

  });
}
