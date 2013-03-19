// Function to be called when the search setting template is ready
function initSearchSetting(){
  jQuery.noConflict();

  (function($){
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
        return searchIn.join(",");
      }
    }


    // Call REST service to save the setting
    $("#btnSave").click(function(){
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
        replace(/%{text}/g, "All"));
      $.each(registry[1], function(i, type){
        if(CONNECTORS[type]) searchInOpts.push(CHECKBOX_TEMPLATE.
          replace(/%{name}/g, "searchInOption").
          replace(/%{value}/g, type).
          replace(/%{text}/g, CONNECTORS[type].displayName));
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
  })(jQuery);

  $ = jQuery; //undo .conflict();
}
