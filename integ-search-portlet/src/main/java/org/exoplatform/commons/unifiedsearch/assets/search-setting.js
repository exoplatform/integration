
function SearchSetting(registry, setting){
  this.connectors = registry[0];
  var _this = this;
  var searchInOpts=[];
  searchInOpts.push("<li><label><input type='checkbox' name='searchInOption' value='all'>Everything</label></li>");
  $.each(registry[1], function(i, type){
    if(_this.connectors[type]) searchInOpts.push("<li><label><input type='checkbox' name='searchInOption' value='" + type + "'>" + _this.connectors[type].displayName + "</label></li>");
  });
  $("#lstSearchInOptions").html(searchInOpts.join(""));

  $(":checkbox[name='searchInOption']").attr('checked', false);
  $.each($(":checkbox[name='searchInOption']"), function(){
    if(-1 != $.inArray(this.value, setting.searchTypes)) {
      $(this).attr('checked', true);
    }
  });
  $("#resultsPerPage").val(setting.resultsPerPage);
  this.updateTxtSearchIn();
  $("#searchCurrentSiteOnly").attr('checked', setting.searchCurrentSiteOnly);
  $("#hideSearchForm").attr('checked', setting.hideSearchForm);
  $("#hideFacetsFilter").attr('checked', setting.hideFacetsFilter);
}


SearchSetting.getSelectedTypes = function(){
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


SearchSetting.prototype.updateTxtSearchIn = function(){
  var _this = this;
  var searchIn = [];
  if($(":checkbox[name='searchInOption'][value='all']").is(":checked")) {
    $("#txtSearchIn").val("Everything");
  } else {
    $.each($(":checkbox[name='searchInOption'][value!='all']:checked"), function(){
      searchIn.push(_this.connectors[this.value].displayName);
    });
    $("#txtSearchIn").val(searchIn.join(", "));
  }
}

