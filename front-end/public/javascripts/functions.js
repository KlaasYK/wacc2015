var map, json;

$.getJSON( "./stations", function( jsondata ) {
  json = jsondata;
  console.log("Loaded location data");
});

$.jgrid.defaults.styleUI = 'Bootstrap';
 
$(document).ready(function () {
    var grid = $("#jqGrid").jqGrid({
            url: '/stations',
            datatype: "json",
             colModel: [
                    { label: 'Pole ID', name: 'id', width: 150 },
                    //{ label: 'Street', name: 'street', width: 90 },
                    //{ label: 'City', name: 'city', width: 100 },
                    { label: 'Status', name: 'status', width: 80, sorttype: 'text' },
                    { label: 'Last active', name:'lastHeartbeat', width: 100, formatter:'date', formatoptions: {srcformat: 'U/1000', newformat:'d/m/Y H:i:s'}}
            ],
            autowidth: true,
            shrinkToFit: true,
            height: 500,
            rowNum: 30,
            pager: "#jqGridPager"
    });

    setInterval(function() {
        grid.trigger("reloadGrid",[{current:true}]);
    }, 10000);

    (function(){
        $('body').on('click', '#top-search > a', function(e){
            e.preventDefault();
            
            $('#header').addClass('search-toggled');
            $('#top-search-wrap input').focus();
        });
        
        $('body').on('click', '#top-search-close', function(e){
            e.preventDefault();
            $('#header').removeClass('search-toggled');
        });
    })();
});

function initMap() {
    map = new google.maps.Map(document.getElementById('map'), {
      center: {lat: 52.0833, lng: 5.1167},
      zoom: 7,
      mapTypeId: google.maps.MapTypeId.ROADMAP,
    });

    // Place markers on map
    for( i = 0; i < json.length; i++) {
        var color = ( json[i].isActive ) ? '#8BC34A' : '#FF5722';
        var latLng = new google.maps.LatLng(json[i].latitude, json[i].longitude);
        var marker = new google.maps.Marker({
            position: latLng,
            map: map,
            icon: {
                path: fontawesome.markers.FLASH,
                scale: 0.4,
                strokeWeight: 0.8,
                strokeColor: 'black',
                strokeOpacity: 1,
                fillColor: color,
                fillOpacity: 1.0,
            },
            url: json[i]._id,
            name: json[i].name,
        });

        google.maps.event.addListener(marker, 'click', function() {
            var location = $.get('location');

            location.done(function() {
                //console.log(location);
               $("#location").html(location.responseText);
            });

            /*
            $.ajax({                                            
                 url: 'location',
                 data: 'id='+this.url+'&name='+this.name+'',    
                 success: function(data)          
                 {   
                    $("#location").html(data);
                 },
                 error: function (xhr, ajaxOptions, thrownError) {
                    console.log(xhr.status);
                    console.log(thrownError);
                 },
            });
            */
        });
    }
};