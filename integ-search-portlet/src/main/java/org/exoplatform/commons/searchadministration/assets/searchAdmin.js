
function initSearchAdmin() {
  $.getJSON("/rest/search/registry", function(registry){
    var row_template = " \
      <tr> \
        <td style='padding-left: 6px;'>%{displayName}</td> \
        <td style='padding-left: 6px;'>%{description}</td> \
        <td style='text-align: center;'> \
          <input type='button' class='ContentType' id='%{id}' value='Enable'> \
          <input type='button' value='Reindex' disabled> \
        </td> \
      </tr> \
    ";

    var connectors = registry[0];
    var searchTypes = registry[1];
    $.each(connectors, function(searchType, connector){
      $("#searchAdmin table").append(row_template.replace(/%{id}/g, connector.searchType)
                                                 .replace(/%{displayName}/g, connector.displayName)
                                                 .replace(/%{description}/g, connector.description));
    });

    $.each(searchTypes, function(i, type){
      $(".ContentType#"+type).val("Disable");
      $(".ContentType#"+type).next().attr("disabled", false);
    });
  });


  $(".ContentType").live("click", function(){
    if("Enable"==$(this).val()) {
      $(this).val("Disable");
      $(this).next().attr("disabled", false);
    } else {
      $(this).val("Enable");
      $(this).next().attr("disabled", true);
    }

    var enabledTypes = [];
//    $.each($(".ContentType"), function(){
//      if("Disable"==this.value) enabledTypes.push(this.id);
//    });

    // To maintain the search types order. TODO: use the table for this (by ordering its rows)
    var order = ["file", "document", "wiki", "page", "discussion", "people", "space", "event", "task", "question", "activity", "jcrNode"];
    $.each(order, function(i, type){
      if("Disable"==$(".ContentType#"+type).val()) enabledTypes.push(type);
    });

    var jqxhr = $.post("/rest/search/enabled-searchtypes", {
      searchTypes:enabledTypes.join(",")
    });

    jqxhr.complete(function(data) {
      if("ok"==data.responseText){
        console.log("Search setting has been saved succesfully.");
      } else {
        alert("Problem occurred when saving your setting: "+data.responseText);
      }
    });
  });

}