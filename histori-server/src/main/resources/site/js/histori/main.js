// Matches value in NexusDAO -- search will never return more than this many results
MAX_SEARCH_RESULTS = 200;

var map;
var mode = 'inspect';
var addRegionWindow = null;
var isClosed = false;
var poly = null;
var markers = [];

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

    var startDate = slider_start_date();
    if (typeof startDate != "undefined") document.getElementById('startDate').value = startDate;

    var endDate = slider_end_date();
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

    addRegionWindow = new google.maps.InfoWindow({ content: addRegionForm() });

    isClosed = false;

    google.maps.event.addListener(map, 'click', function (clickEvent) {

        // always close nexus details upon map click (unless pinned)
        closeNexusDetails();
        closeForm(activeForm);
        $('.ui-tooltip').css('visibility', 'hidden');
        $('.quick-help').css('visibility', 'hidden');

        // disabled for now until we implement geo creation properly
        if (1 == 1) return;

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

    // don't try to restore session/anything if URL indicates to display a help window
    var help = getParameterByName('help');
    if (typeof help == "undefined" || help == null) {
        // Is this a /legal/privacy or /legal/terms link that Apache has rewritten?
        if (window.location.pathname.startsWith('/legal/')) {
            var pos = window.location.pathname.indexOf('/legal/');
            var type = window.location.pathname.substring(pos+1);
            var helpUrl = '/help/legal.html?t='+type;
            $('#helpIframe').attr('src', helpUrl);
            showHelp();
        } else {

            // restore previous session or permalink
            var link_id = getParameterByName('link');
            if (link_id != null && link_id.length >= 5) {
                Histori.restore_permalink(link_id);
            } else {
                if (Histori.has_session_data()) {
                    Histori.restore_session();
                } else {
                    showStandardPermalinks();
                }
            }
        }
    }

}

function init() {
    $(function() {
        google.maps.event.addDomListener(window, "load", initMap);
        var keyParam = getParameterByName('key');
        if (keyParam != null && keyParam.length > 5 && isAnonymous()) {
            showResetPassForm();
        }

        // Setup tooltips
        $( document ).tooltip({
            content: function() {
                var title = this.getAttribute("title");

                // cannot figure out how to disable tooltip on google's recaptcha widget any other way
                if (title.toLowerCase().indexOf("recaptcha widget") != -1) return '';

                // convert pipe chars to line breaks (cannot directly put HTML tags within the title attribute)
                return title.replace(/\|/g, '<br />');
            }
        });

        // Set account button tooltip
        if (!isAnonymous()) {
            Histori.set_account(Histori.account());
        }
    });
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
    return function(e) {
        // fixme: this doesn't work
        // When you click on a timeline marker, the slider control moves to where the marker is
        // I'd like it so that if a marker gets a click event, the slider does not receive it
        //e.stopPropagation();
        //e.preventDefault();
        openNexusDetails(nexusSummaryUuid, 0);
        //return false;
    }
}

// Hash of searchbox_id -> list of markers it generated
var all_markers = [];
var active_markers = {};

// Hash of searchbox_id -> list of summaries it generated
var nexusSummariesByUuid = {};

// useful to just create these once, used in date calculations
var this_year = new Date().getFullYear();
var year1_millis = Date.UTC(this_year, 0);
var year2_millis = Date.UTC(this_year+1, 0);
var millis_in_year = year2_millis - year1_millis;

// called when data is returned from the server, to populate the map with a new set of markers for a particular search box
function canonical_date_to_raw(canonical) {
    var year = canonical.year;
    var month = (typeof canonical.month == 'undefined' || canonical.month == null) ? 0 : canonical.month - 1;
    var day = (typeof canonical.day == 'undefined' || canonical.day == null) ? 1 : canonical.day;
    var point_in_year = new Date(this_year, month, day);
    var millis = point_in_year.getTime();
    var millis_offset = millis - year1_millis;
    var raw = parseFloat(year) + (parseFloat(millis_offset) / parseFloat(millis_in_year));
    return  raw
}

function highlight_timeline_marker(timeline_marker) {
    return function () {
        var hlwidth = 15;
        var highlight = $('<img width="'+hlwidth+'" height="30" class="timeline_marker_highlight" src="iconic/png/caret-bottom-3x.png"/>').css({
            position: 'absolute',
            top: (timeline_marker.position().top - 12) + 'px',
            left: (timeline_marker.position().left + (timeline_marker.width()/2) - (hlwidth/2) + 1) + 'px',
            zIndex: 3
        });
        $('#timeSlider').append(highlight);
        //var bounce_times = 1000;
        //highlight.effect("bounce", {times: bounce_times, distance: 40}, (1000*bounce_times));
    }
}
function unhighlight_timeline_marker(timeline_marker) {
    return function () {
        $('.timeline_marker_highlight').remove();
    }
}

function update_map (searchbox_id) {

    return function (data) {
        hideLoadingSpinner(searchbox_id);

        if (typeof active_markers[searchbox_id] == "undefined") {
            active_markers[searchbox_id] = [];
        }

        // clear existing markers
        remove_markers(searchbox_id);
        slider.remove_markers(searchbox_id);

        var markerImageSrc = rowMarkerImageSrc(searchbox_id);
        if (markerImageSrc == null) {
            console.log('marker was removed ('+searchbox_id+'), cannot add results');
            return;
        }

        if (data && data.results && data.results instanceof Array) {

            for (var i = 0; i < data.results.length; i++) {
                var result = data.results[i];
                //console.log("update_map: result[" + i + "] is: " + result.primary.name);
                if (typeof result.primary != "undefined" && typeof result.primary.geo != "undefined" && result.primary.geo != null && result.primary.geo.type == "Point") {
                    var marker = new google.maps.Marker({
                        position: {lat: result.primary.geo.coordinates[1], lng: result.primary.geo.coordinates[0]},
                        title: result.primary.name,
                        icon: markerImageSrc,
                        map: map
                    });
                    all_markers.push(marker);

                    nexusSummariesByUuid[result.uuid] = result;
                    var clickHandler = newMarkerListener(result.uuid);
                    if (clickHandler == null) {
                        console.log("update_map: error adding: "+result.uuid);
                        continue;
                    }
                    marker.addListener('click', clickHandler);

                    // convert start/end instant to raw date value (year.fraction)
                    var start = canonical_date_to_raw(result.primary.timeRange.startPoint);
                    var end = (typeof result.primary.timeRange.endPoint == 'undefined'
                                || result.primary.timeRange.endPoint == null
                                || result.primary.timeRange.startPoint.instant == result.primary.timeRange.endPoint.instant)
                        ? null : canonical_date_to_raw(result.primary.timeRange.endPoint);

                    var timeline_marker = slider.add_marker(searchbox_id, marker, start, end, result.primary.name, markerImageSrc, clickHandler);

                    marker.addListener('mouseover', highlight_timeline_marker(timeline_marker));
                    marker.addListener('mouseout', unhighlight_timeline_marker(timeline_marker));

                    active_markers[searchbox_id].push(marker);
                }
            }

            if (data.results.length >= MAX_SEARCH_RESULTS) {
                slider.show_more_icon(searchbox_id);
            } else {
                slider.clear_more_icon(searchbox_id);
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
    slider.update_markers(searchbox_id, imageSrc);
}

function remove_markers(searchbox_id) {
    var markers = active_markers[searchbox_id];

    if (typeof markers == "undefined" || markers == null || !is_array(markers)) return;

    for (var i = 0; i < active_markers[searchbox_id].length; i++) {
        active_markers[searchbox_id][i].setMap(null);
    }
    active_markers[searchbox_id] = [];

    slider.remove_markers(searchbox_id);
}

function remove_all_markers () {
    for (var i=0; i<all_markers.length; i++) {
        if (typeof all_markers[i].setMap != 'undefined') {
            all_markers[i].setMap(null);
        } else {
            console.log('marker had no setMap: '+all_markers[i]);
        }
    }
    slider.remove_all_markers();
    active_markers = {};
    all_markers = [];
}

function bring_markers_to_front(searchbox_id) {
    var markers = active_markers[searchbox_id];

    if (typeof markers == "undefined" || markers == null || !is_array(markers)) return;

    var i;
    for (i=0; i<all_markers.length; i++) {
        all_markers[i].setZIndex(1);
    }
    for (i = 0; i < active_markers[searchbox_id].length; i++) {
        active_markers[searchbox_id][i].setZIndex(2);
    }
    slider.bring_markers_to_front(searchbox_id);
}