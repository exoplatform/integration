(function($){
function initSearchAdmin() {
  
    $.getJSON("/rest/search/registry", function(registry){
      var row_template = " \
        <tr> \
          <td>%{displayName}</td> \
          <td>%{description}</td> \
          <td class='center'> \
            <input type='button' class='btn btn-mini contentType' id='%{id}' value='Enable'> \
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
        $(".contentType#"+type).val("Disable");
        //$(".ContentType#"+type).next().attr("disabled", false);
      });
    });


    $(".contentType").live("click", function(){
      if("Enable"==$(this).val()) {
        $(this).val("Disable");
        //$(this).next().attr("disabled", false);
      } else {
        $(this).val("Enable");
        //$(this).next().attr("disabled", true);
      }

      var enabledTypes = [];
      $.each($(".contentType"), function(){
        if("Disable"==this.value) enabledTypes.push(this.id);
      });
      
      var jqxhr = $.post("/rest/search/enabled-searchtypes/"+enabledTypes, {
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


  initSearchAdmin();

})($);