var map, json;
var poledata = Array();
var notifications = Array();

var pstatus = {};
pstatus.DOWN = 0;
pstatus.IDLE = 1;
pstatus.CHARGING = 2;

var DOWNTIMEOUT = 25000;

$.jgrid.defaults.styleUI = 'Bootstrap';

$('#locationinfo').hide();

function setTimeOutClosure(poleid) {
    return setTimeout(function(){
        poledata[poleid].status = pstatus.DOWN;

        // Update grid
        var rowData = $('#jqGrid').jqGrid('getRowData', poleid);
        rowData.status = 0;
        $('#jqGrid').jqGrid('setRowData', poleid, rowData);

        // TODO: Update notifications
        notifications.unshift([0, poleid]);
        updateNotifications();
    },DOWNTIMEOUT);
}

$(document).ready(function () {
    var socket = new WebSocket('ws://' + location.host + "/stationsfeed");

    // REMOVE test
    socket.onopen = function () {
        console.log('Websocket Connected!');
    };

    socket.onmessage = function (event) {
        var pole = JSON.parse(event.data);

        // Update grid
        if ( $('#jqGrid').jqGrid('getInd', pole.id) )
            $('#jqGrid').jqGrid('setRowData', pole.id, pole);
        else
            $('#jqGrid').jqGrid('addRowData', pole.id, pole);

        if (poledata[pole.id] && poledata[pole.id].timeout) {clearTimeout(poledata[pole.id].timeout); }

        poledata[pole.id] = pole;
        poledata[pole.id].timeout = setTimeOutClosure(pole.id);
    };

    socket.onclose = function () {
        console.log('Websocket Connection closed');
    };

    // Close the connection when leaving the page
    $(window).bind('beforeunload', function(){
        socket.close();
    });

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

    var notificationbutton = document.getElementById('notificationbutton');
    notificationbutton.addEventListener('click', function() {
        $('#notificationcount').css('visibility', 'hidden');
        notifications.length = 0;
    }, false);
});

// Load grid
var grid = $("#jqGrid").jqGrid({
    url: '/stations',
    datatype: "json",
    colModel: [
        { label: 'Pole ID', name: 'id', width: 150, formatter: 'showLink', formatoptions:{ onClick: function() { alert('test'); } } },
        //{ label: 'Street', name: 'street', width: 90 },
        //{ label: 'City', name: 'city', width: 100 },
        { label: 'Status', name: 'status', width: 80, sorttype: 'text',  formatter: convertStatus},
        //{ label: 'Last active', name:'lastHeartbeat', width: 100, formatter:'date', formatoptions: {srcformat: 'U/1000', newformat:'d/m/Y H:i:s'}}
    ],
    autowidth: true,
    shrinkToFit: true,
    height: 500,
    rowNum: 100,
    pager: "#jqGridPager",
    onSelectRow: function(id){
        showSession(id);
    }
});

$.getJSON( "./stations", function( jsondata ) {
    json = jsondata;
    console.log("Loaded location data");
    // structure which prevents looping in heartbeat updates ;)
    for (var i = 0; i < json.length; i++) {
        var pole = json[i];
        poledata[pole.id] = pole;
        var curtime = new Date().getTime();
        if (pole.status != pstatus.DOWN && (pole.lastHeartbeat + DOWNTIMEOUT) > curtime) {
            // Set timeout
            poledata[pole.id].timeout = setTimeOutClosure(pole.id);
        } else {
            poledata[pole.id].status = pstatus.DOWN;

            // Set pole as offline in grid
            var rowData = $('#jqGrid').jqGrid('getRowData', pole.id);
            rowData.status = 0;
            $('#jqGrid').jqGrid('setRowData', pole.id, rowData);
        }
    }
});

/*
$.getJSON( "./sessions/TEST1234/1443816282212/1444816487220", function( jsondata ) {
    console.log(jsondata);
    $('#station-id').html(jsondata[0].poleid);
    createSessionGrid(jsondata);
});

$.getJSON( "./sessions/POLE-0002/1443816282212/1444816487220", function( jsondata ) {
    console.log("x");
    jQuery("#locationGrid")
        .jqGrid('setGridParam',
        {
            datatype: 'local',
            data:jsondata
        })
        .trigger("reloadGrid");

});
*/

var locationgrid;

function showSession(poleid)
{
    $('#map').slideUp();
    $('#locationinfo').slideDown();
    $('#station-id').html(poleid);
    $.getJSON( "./sessions/"+poleid+"/"+(Date.now() - (60*60*24*7))+"/"+Date.now()+"", function( jsondata ) {

        if ( locationgrid != null )
        {
            $("#locationGrid").jqGrid("clearGridData", true).trigger("reloadGrid");
            $("#locationGrid").jqGrid('setGridParam',{datatype: 'local',data:jsondata}).trigger("reloadGrid");
        }
        else
        {
            createSessionGrid(jsondata);
            $("#locationGrid").jqGrid('setGridParam',{datatype: 'local',data:jsondata}).trigger("reloadGrid");
        }
    });
}



function createSessionGrid(data)
{
    locationgrid = $("#locationGrid").jqGrid({
        url: data,
        datatype: "local",
        colModel: [
            { label: 'Start', name:'startDate', width: 100, formatter:'date', formatoptions: {srcformat: 'U/1000', newformat:'d/m/Y H:i:s'}},
            { label: 'End', name:'endDate', width: 100, formatter:'date', formatoptions: {srcformat: 'U/1000', newformat:'d/m/Y H:i:s'}},
            { label: 'kwh', name: 'kwh', width: 50, formatter:'number', formatoptions: {decimalSeparator:".", thousandsSeparator: ",", decimalPlaces: 2, defaultValue: '0.00'}},
            { label: 'Price', name: 'price', width: 50, formatter:'currency', formatoptions: {prefix:'€', thousandsSeparator:'.'}},
        ],
        autowidth: true,
        shrinkToFit: true,
        sortname: 'startDate',
        sortorder: 'desc',
        height: 500,
        rowNum: 100,
        pager: "#locationPager"
    });
}

function updateNotifications()
{
    $('#notifications').html("");
    for ( var item in notifications )
    {
        if ( $('#notifications > a').length === 6 ) { break; }
        var warning = getWarning(notifications[item][0], notifications[item][1])
        var notification = "<a class=\"lv-item\" href=\"\">" +
            "<div class=\"lv-title\">" + warning[0] + "</div>" +
            "<small class=\"lv-small\">" + warning[1] + "</small>" +
            "</a>";
        $('#notifications').append(notification);
    }
    $('#notificationcount').css('visibility', 'visible').text(notifications.length);
}

function getWarning(code, poleid)
{
    switch (code)
    {
        case 0:
            return ["Pole offline!", "Pole " + poleid + " seems to be offline."];
        case 1:
            return ["Pole added", "Pole " + poleid + " showed up for the first time."];
    }
}

function convertStatus(cellValue)
{
    switch(cellValue)
    {
        case 0:
            return "Offline";
        case 1:
            return "Idle";
        case 2:
            return "Charging";
        default:
            return "Unknown";
    }
}

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
        });
    }

    google.maps.event.addDomListener(window, "resize", function() {
        var center = map.getCenter();
        google.maps.event.trigger(map, "resize");
        map.setCenter(center);
    });
};

