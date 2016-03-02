MAX_SLIDER = 10000.0;

var map;
var mode = 'inspect';
var addRegionWindow = null;
var isClosed = false;
var poly = null;
var markers = [];
var timeRangeSlider;
var timeSlider;

// get locale -- todo: fetch from server at /locale
locale = "en-US";
language = "en";
var localizer = {
    numberFormatter: function (val) {
        if (val > 10000) {
            return val.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        }
        return val;
    }
};

var openAddRegionWindow = function (map, marker) {
    addRegionWindow.open(map, marker);

    var startDate = timeSlider.dates[0];
    if (typeof startDate != "undefined") document.getElementById('startDate').value = startDate;

    var endDate = timeSlider.dates[1];
    if (typeof endDate != "undefined") document.getElementById('endDate').value = endDate;

    // Initialize autocomplete with local lookup:
    $('#autocomplete_tag').devbridgeAutocomplete({
        serviceUrl: Api.autocomplete("no-type"),
        minChars: 1,
        onSelect: function (suggestion) {
            $('#ac_selection_tag').html('You selected: ' + suggestion.value + ', ' + suggestion.data.category);
        },
        showNoSuggestionNotice: true,
        noSuggestionNotice: 'Sorry, no matching results',
        groupBy: 'category'
    });

    // Initialize autocomplete with local lookup:
    $('#autocomplete_worldevent').devbridgeAutocomplete({
        serviceUrl: Api.autocomplete(),
        minChars: 1,
        onSelect: function (suggestion) {
            $('#ac_selection_worldevent').html('You selected: ' + suggestion.value + ', ' + suggestion.data.category);
        },
        showNoSuggestionNotice: true,
        noSuggestionNotice: 'Sorry, no matching results',
        groupBy: 'category'
    });
};

function initMap () {

    map = new google.maps.Map(document.getElementById('map'), {
        center: new google.maps.LatLng(20.0, 0.0),
        zoom: 2,
        mapTypeId: google.maps.MapTypeId.HYBRID,
        scaleControl: true
    });

    $(window).mouseup(function() {
        //alert("This will show after mousemove and mouse released.");
        //console.log("CLEANUP: onmouseup -- cancelling rotation handler");
        window.clearInterval(rotate_handler);
        rotate_handler = window.setInterval(function () {}, 5000);
    });

    // rotation buttons
    Histori.set_session("mapImg_rotate_amount", 0.0);
    $('#clockwise').mousedown(createRotationHandler('mapImg', +1));
    $('#anticlockwise').mousedown(createRotationHandler('mapImg', -1));

    // file upload handler
    document.getElementById('fileImageUpload').addEventListener('change', function(e) {
        var file = this.files[0];
        var xhr = new XMLHttpRequest();
        (xhr.upload || xhr).addEventListener('progress', function(e) {
            var done = e.position || e.loaded;
            var total = e.totalSize || e.total;
            console.log('xhr progress: ' + Math.round(done/total*100) + '%');
        });
        xhr.addEventListener('load', function(e) {
            console.log('xhr upload complete', e, this.responseText);
        });
        Api.upload_image(xhr, file, function (xhr) {
            var src = JSON.parse(xhr.response).url;

            var container = $('#mapImageContainer');
            container.css("zIndex", 1);
            var elem = $('#mapImageContainer > .container');
            //var xformUri = Api.transform_image(src, 100, 200);
            var xformUri = src;

            $('#mapImg')[0].src = xformUri;

            elem.css('top', 0);
            elem.css('left', 0);
            elem.css('width', 400);
            elem.css('height', 300);
            elem.draggable();
            elem.find('.mapImage:first').resizable();
        });
    });

    var sliderContainer = document.getElementById('sliderContainer');
    var thisYear = new Date().getFullYear();
    timeSlider = new TimeRangeControl(-100000, thisYear);

    sliderContainer.index = 1;
    sliderContainer.style['padding-top'] = '10px';
    sliderContainer.style['padding-bottom'] = '-10px';
    map.controls[google.maps.ControlPosition.BOTTOM_CENTER].push(sliderContainer);

    // Create the slider control
    var sliderScreenSize = (($(window).width() * 0.75)/1);

    document.getElementById('sliderTable').style.width = sliderScreenSize+"px";
    document.getElementById('sliderCell').style.width = "70%";
    document.getElementById('sliderCell').style.textAlign = "center";
    document.getElementById('sliderStartLabel').innerHTML = timeSlider.label(0);
    document.getElementById('sliderEndLabel').innerHTML = timeSlider.label(MAX_SLIDER);

    //var sliderSize = (sliderScreenSize * 0.70)/1;
    var sliderSize = document.getElementById('sliderCell').offsetWidth;

    timeRangeSlider = new dhtmlXSlider({
        parent: "sliderObj",
        linkTo: ["sliderStart", "sliderEnd"],
        step: 1,
        min: 0,
        max: MAX_SLIDER,
        value: [0, MAX_SLIDER],
        range: true,
        size: sliderSize
    });
    timeSlider.setSliderLabels(timeRangeSlider.getValue());

    timeRangeSlider.attachEvent("onChange", function () { timeSlider.updateHistoryRange(); refresh_map(); } );
    document.body.onkeydown = function (e) {
        e = e || window.event;
        if (e.keyCode == '37') {
            // left arrow
            timeRangeSlider.setValue(timeSlider.decrementLast());
            timeSlider.updateHistoryRange();
            refresh_map();
        }
        else if (e.keyCode == '39') {
            // right arrow
            timeRangeSlider.setValue(timeSlider.incrementLast());
            timeSlider.updateHistoryRange();
            refresh_map();
        }
    };
    zoomToDates(-10000, thisYear);
    zoomToDates(-4000, thisYear);
    zoomToDates(1500, thisYear);

    addRegionWindow = new google.maps.InfoWindow({ content: addRegionForm() });

    isClosed = false;

    google.maps.event.addListener(map, 'click', function (clickEvent) {

        // always close nexus details upon map click (unless pinned)
        closeNexusDetails();
        closeForm(activeForm);

        if (mode == 'inspect') {
            // todo: remove this
            return;
        }
        if (mode == 'addEvent') {
            console.log('show addEvent dialog here');
            return;
        }
        if (mode != 'addRegion') {
            console.log('doing '+mode+"...");
            return;
        }
        if (isClosed) return;
        if (poly == null) poly = new google.maps.Polyline({ map: map, path: [], strokeColor: "#FF0000", strokeOpacity: 1.0, strokeWeight: 2 });

        var markerIndex = poly.getPath().length;
        var isFirstMarker = markerIndex === 0;
        var marker = new google.maps.Marker({ map: map, position: clickEvent.latLng, draggable: true });
        if (isFirstMarker) {
            google.maps.event.addListener(marker, 'click', function () {
                if (isClosed) return;
                var path = poly.getPath();
                poly.setMap(null);
                poly = new google.maps.Polygon({ map: map, path: path, strokeColor: "#FF0000", strokeOpacity: 0.8, strokeWeight: 2, fillColor: "#FF0000", fillOpacity: 0.35 });
                isClosed = true;
                openAddRegionWindow(map, marker);
                poly.addListener('click', function () {
                    openAddRegionWindow(map, marker);
                });
            });
        }
        google.maps.event.addListener(marker, 'drag', function (dragEvent) {
            poly.getPath().setAt(markerIndex, dragEvent.latLng);
        });
        var path = poly.getPath();
        path.push(clickEvent.latLng);
        //poly = new google.maps.Polygon({ map: map, path: path, strokeColor: "#FF0000", strokeOpacity: 0.8, strokeWeight: 2, fillColor: "#FF0000", fillOpacity: 0.35 });
        markers.push(marker);
    });

    google.maps.event.addListener(map, 'bounds_changed', function () {
        refresh_map();
    });
}

function init() {
    $(document).ready(function () {
        google.maps.event.addDomListener(window, "load", initMap);
        var keyParam = getParameterByName('key');
        if (keyParam != null && keyParam.length > 5 && isAnonymous()) {
            showResetPassForm();
        }
        initSearchForm();
    });
}

modeButtons = ['btnInspect', 'btnAddRegion', 'btnImageRegion', 'btnAddEvent'];

function setMode (m, button) {
    mode = m;
    var hasButton = (typeof button != "undefined" && button != null);
    if (hasButton) button.style['font-weight'] = "bold";
    for (var i=0; i<modeButtons.length; i++) {
        if (!hasButton || button.id != modeButtons[i]) document.getElementById(modeButtons[i]).style['font-weight'] = "normal";
    }
    if (m == 'imageRegion') $('#fileImageUpload').click();
}

function addRegion () {
    console.log('createHistori!');
    removePolygon();
}

function removePolygon() {
    poly.setMap(null);
    addRegionWindow.close();
    if (markers != null) {
        for (var i = 0; i < markers.length; i++) {
            markers[i].setMap(null);
        }
        markers = [];
    }
    isClosed = false;
    poly = null;
}

function cancelAddRegion () {
    console.log('cancelAddRegion ...');
    removePolygon();
}

function addRegionForm () {
    return '<div id="content">'+
        '<div id="authMessageSlot"></div>'+
        '<div id="authFormSlot"></div>'+
        '<div id="bodyContent">'+
        '<p><form onsubmit="return false;">' +
        'Event: <div><input type="text" name="eventName" id="autocomplete_worldevent"/></div><div id="ac_selection_worldevent"></div><br/>' +
        'Start: <input id="startDate" type="text" name="startDate"/><br/>' +
        'End: <input id="endDate" type="text" name="endDate"/><br/>' +
        'Tags: <div><input type="text" name="country" id="autocomplete_tag"/></div><div id="ac_selection_tag"></div><br/>' +
        '<br/>' +
        '<div id="tag_container"></div>' +
        '<button id="btnCreate" value="add" onclick="addRegion(this.form)">create</button><br/>'+
        '<button id="btnCancel" value="cancel" onclick="cancelAddRegion()">cancel</button><br/>'+
        '</form></p>'+
        '</div>'+
        '</div>';
}

function newMarkerListener(nexusSummaryUuid) {
    return function() {
        openNexusDetails(nexusSummaryUuid, 0);
    }
}

// Hash of searchbox_id -> list of markers it generated
var active_markers = {};

// Hash of searchbox_id -> list of summaries it generated
var nexusSummariesByUuid = {};

// called when data is returned from the server, to populate the map with a new set of markers for a particular search box
function update_map (searchbox_id) {
    return function (data) {
        hideLoadingSpinner(searchbox_id);
        if (data && data.results && data.results instanceof Array) {

            if (typeof active_markers[searchbox_id] == "undefined") {
                active_markers[searchbox_id] = [];
            }

            // clear existing markers
            remove_markers(searchbox_id);

            for (var i = 0; i < data.results.length; i++) {
                var result = data.results[i];
                //console.log("update_map: result[" + i + "] is: " + result.primary.name);
                if (typeof result.primary != "undefined" && typeof result.primary.geo != "undefined" && result.primary.geo != null && result.primary.geo.type == "Point") {
                    var marker = new google.maps.Marker({
                        position: {lat: result.primary.geo.coordinates[1], lng: result.primary.geo.coordinates[0]},
                        title: result.primary.name,
                        icon: rowMarkerImageSrc(searchbox_id),
                        map: map
                    });

                    nexusSummariesByUuid[result.uuid] = result;
                    var clickHandler = newMarkerListener(result.uuid);
                    if (clickHandler == null) {
                        console.log("update_map: error adding: "+result.uuid);
                        continue;
                    }
                    marker.addListener('click', clickHandler);

                    active_markers[searchbox_id].push(marker);
                }
            }
        }
    }
}

function update_markers(searchbox_id, imageSrc) {
    var markers = active_markers[searchbox_id];

    if (typeof markers == "undefined" || markers == null || !is_array(markers)) return;

    for (var i=0; i<markers.length; i++) {
        markers[i].setIcon(imageSrc);
    }

}

function remove_markers(searchbox_id) {
    var markers = active_markers[searchbox_id];

    if (typeof markers == "undefined" || markers == null || !is_array(markers)) return;

    for (var i = 0; i < active_markers[searchbox_id].length; i++) {
        active_markers[searchbox_id][i].setMap(null);
    }
    active_markers[searchbox_id] = [];
}