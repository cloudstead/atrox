MAX_SLIDER = 10000.0;

var map;
var mode = 'inspect';
var addRegionWindow = null;
var isClosed = false;
var poly = null;
var markers = [];
var timeRangeSlider;
var sliderControl;

function is_array (x) {
    return Object.prototype.toString.call( x ) === '[object Array]'
}

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

function updateAuthMessage() {
    var authMessageSlot = document.getElementById('authMessageSlot');
    if (get_token() == NO_TOKEN || ((typeof Histori.account == 'undefined') || (typeof Histori.account.email == 'undefined'))) {
        authMessageSlot.innerHTML
            = 'Currently anonymous. <a href="." onclick="showLoginForm(); return false;">Sign in</a> or '
            + '<a href="." onclick="showRegistrationForm(); return false;">Sign up</a>';
    } else {
        authMessageSlot.innerHTML
            = 'Currently logged in as ' + Histori.account.email + ' <a href="." onclick="Histori.logout(); return false;">Log out</a>';
    }
}
var openAddRegionWindow = function (map, marker) {
    addRegionWindow.open(map, marker);

    var startDate = sliderControl.sliderDates[0];
    if (typeof startDate != "undefined") document.getElementById('startDate').value = startDate;

    var endDate = sliderControl.sliderDates[1];
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


    updateAuthMessage();
};

function showLoginForm () {
    var loginForm = document.getElementById('loginForm');
    var authFormSlot = document.getElementById('authFormSlot');
    authFormSlot.innerHTML = '';
    authFormSlot.appendChild(loginForm);
}

function closeLoginForm () {
    var loginForm = document.getElementById('loginForm');
    var loginContainer = document.getElementById('loginContainer');
    loginContainer.appendChild(loginForm);
    updateAuthMessage();
}

function handleLoginError () {
    var authMessageSlot = document.getElementById('authMessageSlot');
    var loginContainer = document.getElementById('loginContainer');
    var loginForm = document.getElementById('loginForm');
    authMessageSlot.innerHTML = '<b>oops, there was an error</b>';
    loginContainer.appendChild(loginForm)
    updateAuthMessage();
}

function showRegistrationForm () {
    var regForm = document.getElementById('regForm');
    var authFormSlot = document.getElementById('authFormSlot');
    authFormSlot.innerHTML = '';
    authFormSlot.appendChild(regForm);
}

function closeMapImages () {
    var container = $('#mapImageContainer');
    container.css('zIndex', -1);
}

MONTHS = [null, 'January', 'February', 'March', 'April','May','June','July','August','September','October','November','December'];

function formatEditTimePoint(date) {
    if (typeof date == "undefined" || date == null) return "";
    var year = date.year;
    var month = (typeof date.day == "undefined" || date.month == null) ? "" : "-" + date.month;
    var day = (typeof date.day == "undefined" || date.day == null) ? "" : "-" + date.day;

    return year + month + day;
}
function formatTimePoint(date) {
    if (typeof date == "undefined" || date == null) return "";
    var year = date.year;
    var day = (typeof date.day == "undefined" || date.day == null) ? "" : date.day;
    var month = (typeof date.day == "undefined" || date.month == null) ? "" : MONTHS[date.month];

    return day + " " + month + " " + year;
}
function formatRange (range) {
    // todo: use locale-specific date formatting
    if (typeof range == "undefined" || range == null) return "";
    if (typeof range.startPoint == "undefined" || range.startPoint == null) return "";

    var start = range.startPoint;
    if (typeof start.year == "undefined" || start.year == null) return "";
    var value = formatTimePoint(start);

    if (typeof range.endPoint == "undefined" || range.endPoint == null) return value;

    var end = range.endPoint;
    if (typeof end.year == "undefined" || end.year == null) return value;
    return value + " - " + formatTimePoint(end);
}

TAG_TYPES = ['event_type', 'world_actor', 'result', 'impact', 'person', 'event', 'citation', 'idea', 'meta'];
TAG_TYPE_NAMES = ['event types', 'world actors', 'results', 'impacts', 'persons', 'events', 'citations', 'ideas', 'meta'];
var activeNexusSummary = null;

function showLoadingMessage (message) {
    var loadingContainer = $('#nexusLoadingContainer');
    var hasLoadingContainer = loadingContainer.length ? true : false;
    if (!hasLoadingContainer) {
        loadingContainer = $('<div id="nexusLoadingContainer">'+message+'</div>');
    } else {
        loadingContainer.html(message);
    }
    $('#nexusTitleContainer').append(loadingContainer);
}
function hideLoadingMessage () {
    $('#nexusLoadingContainer').remove();
}

function openNexusDetails (nexusSummary, tries) {

    closeEditNexusDetails();

    if (typeof nexusSummary == "undefined" || nexusSummary == null) return;

    activeNexusSummary = nexusSummary;
    var nexus = activeNexusSummary.primary;

    $('#nexusNameContainer').html(nexus.name);
    Api.owner_name(nexus.owner, '#nexusAuthorContainer', "created by: ");
    $('#nexusRangeContainer').html(formatRange(nexus.timeRange));
    //if (typeof nexus.nexusType != "undefined" && nexus.nexusType != null) {
    //    $('#nexusTypeContainer').html("("+nexus.nexusType+")");
    //}

    var otherVersionCount = 0;
    if (typeof nexus.others != "undefined" && is_array(nexus.others)) {
        otherVersionCount = nexus.others.length;
    }
    var btnNexusVersions = $('#btn_nexusVersions');
    switch (otherVersionCount) {
        case 0:
            btnNexusVersions.css('visibility', 'hidden');
            break;
        case 1:
            btnNexusVersions.html('1 other version');
            btnNexusVersions.css('visibility', 'visible');
            break;
        default:
            btnNexusVersions.html(otherVersionCount + ' other versions');
            btnNexusVersions.css('visibility', 'visible');
            break;
    }

    var tagsContainer = $('#nexusTagsContainer');
    tagsContainer.empty();

    if (nexusSummary.incomplete) {
        if (tries < 5) {
            window.setTimeout(function () {
                console.log('trying to find nexus, try #' + tries);
                Api.find_nexus(nexus.uuid, function (data) {
                    console.log('find_nexus for try #' + tries + ' returned nexus, incomplete=' + data.incomplete);
                    openNexusDetails(data, tries + 1);
                }, null);
            }, tries * 2000);
            showLoadingMessage("(data still loading)");

        } else {
            showLoadingMessage("(data loading failed after " + tries + " tries)");
        }
    } else {
        hideLoadingMessage();
        if (typeof nexus.tags != "undefined" && is_array(nexus.tags)) {
            var names = [];
            var tagsTable = $('<table id="nexusTagsTable">');

            tagsContainer.append(tagsTable);

            var tagsByType = {};
            for (var i = 0; i < nexus.tags.length; i++) {
                var tag = nexus.tags[i];
                if (typeof tagsByType[tag.tagType] == "undefined") {
                    tagsByType[tag.tagType] = [];
                }
                tagsByType[tag.tagType].push(tag);
            }

            var tbody = $('<tbody>');
            tagsTable.append(tbody);
            for (var typeIndex = 0; typeIndex < TAG_TYPES.length; typeIndex++) {
                var tagType = TAG_TYPES[typeIndex];
                if (typeof tagsByType[tagType] == "undefined") continue;

                var tags = tagsByType[tagType];

                var tagTypeName = TAG_TYPE_NAMES[typeIndex];
                if (tags.length == 1 && tagTypeName.endsWith("s")) {
                    tagTypeName = tagTypeName.substring(0, tagTypeName.length - 1);
                }

                var tagRow = $('<tr class="tagTypeRow">');
                tagRow.append($('<td class="tagTypeCell">' + tagTypeName + '</td>'));
                tbody.append(tagRow);

                var listOfTags = "";
                for (var j = 0; j < tags.length; j++) {
                    var nexusTagId = 'nexusTag_' + tags[j].uuid + '_' + tags[j].tagName;
                    listOfTags += "<div class='nexusTag'><div id='" + nexusTagId + "'>" + tags[j].displayName + "</div>";
                    if (typeof tags[j].values != "undefined" && is_array(tags[j].values)) {
                        var prevField = '';
                        var numValues = tags[j].values.length;
                        for (var k = 0; k < numValues; k++) {
                            var schemaVal = tags[j].values[k];
                            var displayField;
                            var schemaValueId = nexusTagId + '_' + k + '_' + schemaVal.value;
                            var schemaTypeIndex = $.inArray(schemaVal.field, TAG_TYPES);
                            if (schemaTypeIndex != -1) {
                                displayField = TAG_TYPE_NAMES[schemaTypeIndex];
                                names.push({tag: schemaVal.value, id: schemaValueId});
                            } else {
                                displayField = schemaVal.field;
                            }
                            if (numValues > 1 && prevField != displayField) {
                                //listOfTags += "<div class='schema_field'>"+ displayField.replace('_', ' ') + "</div>";
                                prevField = displayField;
                            }
                            listOfTags += "<div id='" + schemaValueId + "' class='schema_value'>" + schemaVal.value.replace('_', ' ') + "</div>";
                        }
                    }
                    listOfTags += "</div>";
                    names.push({tag: tags[j].tagName, id: nexusTagId});
                }
                tagRow.append($('<td class="tagCell">' + listOfTags + '</td>'));
            }
            Api.resolve_tags(names, update_tag_display_name);
        }
    }
    var container = $('#nexusDetailsContainer');
    container.css('zIndex', 1);
}

function update_tag_display_name (id, displayName) {
    if (displayName.lastIndexOf('http://', 0) === 0 || displayName.lastIndexOf('https://', 0) === 0) {
        displayName = '<a target="_blank" href=' + displayName + '>' + window.decodeURI(displayName) + "</a>";
    }
    $('#'+id).html(displayName);
}

function closeNexusDetails () {
    var container = $('#nexusDetailsContainer');
    container.css('zIndex', -1);
}

function editNexusDetails () {
    closeNexusDetails();

    if (typeof activeNexusSummary == "undefined" || activeNexusSummary == null) return;

    var nexus = activeNexusSummary.primary;

    $('#nexusEditNameContainer').html(nexus.name);
    $('#nexusRangeStart').val(formatEditTimePoint(nexus.timeRange.startPoint));
    $('#nexusRangeEnd').val(formatEditTimePoint(nexus.timeRange.endPoint));


    var container = $('#nexusEditContainer');
    container.css('zIndex', 1);
}

function closeEditNexusDetails () {
    var container = $('#nexusEditContainer');
    container.css('zIndex', -2);
}

function saveEditNexusDetails () {
    // todo
}

function viewNexusVersions () {
    // todo
}

map_image_mode = 'image';
function toggleMapImageMode() {
    if (map_image_mode == 'image') {
        map_image_mode = 'map';
        $('#mapImg').css({'pointer-events': 'none'});
        $('.container').css({'pointer-events': 'none'});
        $('#btnMapImageMode').html('unfreeze image');
    } else {
        map_image_mode = 'image';
        $('#mapImg').css({'pointer-events': 'visible'});
        $('.container').css({'pointer-events': 'visible'});
        $('#btnMapImageMode').html('freeze image');
    }
}

// dummy-noop at first
rotate_handler = window.setInterval(function () {}, 5000);

function createRotationHandler (id, direction) {
    //console.log("ONMOUSEDOWN: createRotationHandler: creating for "+id+"/"+direction);
    var element = $('#'+id);

    return function (e) {
        Histori.set_session(id+"_rotate_start", Date.now());

        window.clearInterval(rotate_handler);
        rotate_handler = window.setInterval(function () {
            var degree = parseFloat(Histori.get_session(id+"_rotate_amount", 0.0));
            if (degree == null) degree = 0.0;
            Histori.set_session(id+"_rotate_amount", degree);

            var start = parseInt(Histori.get_session(id+"_rotate_start", Date.now()));
            if (start == null || isNaN(start)) start = Date.now();
            Histori.set_session(id+"_rotate_start", start);

            var duration = parseInt(Date.now() - start);
            if (duration > 15000) {
                window.clearInterval(rotate_handler);
                rotate_handler = window.setInterval(function () {}, 5000);
            }

            var delta = Math.min(5.0, 50.0 * Math.pow(duration/10000.0, 3));
            delta *= (1.0) * direction;
            degree += delta;
            Histori.set_session(id+"_rotate_amount", degree);

            console.log("adjusted "+id+" angle by delta="+delta+", now="+degree+" (duration="+duration+")");

            element.css({ WebkitTransform: 'rotate(' + degree + 'deg)'});
            element.css({ '-moz-transform': 'rotate(' + degree + 'deg)'});

        }, 50);

        // Using return false prevents browser's default,
        // often unwanted mousemove actions (drag & drop)
        return false;
    }
}
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

    // Create the DIV to hold the control and call the CenterControl() constructor
    // passing in this DIV.
    var sliderContainer = document.getElementById('sliderContainer');
    sliderControl = new TimeRangeControl(sliderContainer, map, new google.maps.LatLng(0.0, 0.0));
    var thisYear = new Date().getFullYear();
    sliderControl.setRangeOrigin(-100000);
    sliderControl.setRangeCurrent(thisYear);

    sliderContainer.index = 1;
    sliderContainer.style['padding-top'] = '10px';
    sliderContainer.style['padding-bottom'] = '-10px';
    map.controls[google.maps.ControlPosition.BOTTOM_CENTER].push(sliderContainer);

    // Create the slider control
    var sliderScreenSize = (($(window).width() * 0.75)/1);
    var sliderSize = (sliderScreenSize * 0.70)/1;

    timeRangeSlider = new dhtmlXSlider({
        parent: "sliderObj",
        linkTo: ["sliderOrigin", "sliderCurrent"],
        step: 1,
        min: 0,
        max: MAX_SLIDER,
        value: [0, MAX_SLIDER],
        range: true,
        size: sliderSize
    });
    sliderControl.setSliderLabels(timeRangeSlider.getValue());

    document.getElementById('sliderTable').style.width = sliderScreenSize+"px";
    document.getElementById('sliderPre').style.width = ((sliderScreenSize*0.125)/1)+"px";
    document.getElementById('sliderPost').style.width = ((sliderScreenSize*0.125)/1)+"px";
    document.getElementById('sliderCell').style.width = sliderScreenSize+"px";
    document.getElementById('sliderCell').style.textAlign = "center";
    document.getElementById('sliderOriginLabel').innerHTML = sliderControl.label(0);
    document.getElementById('sliderCurrentLabel').innerHTML = sliderControl.label(MAX_SLIDER);
    var listener = function(){
        var vals = timeRangeSlider.getValue();
        sliderControl.setSliderLabels(vals);
        document.getElementById('sliderOriginLabel').innerHTML = sliderControl.sliderLabels(0);
        document.getElementById('sliderCurrentLabel').innerHTML = sliderControl.sliderLabels(1);
    };
    timeRangeSlider.attachEvent("onChange", function () { sliderControl.updateHistoryRange(); refresh_map(); } );
    document.body.onkeydown = function (e) {
        e = e || window.event;
        if (e.keyCode == '37') {
            // left arrow
            timeRangeSlider.setValue(sliderControl.decrementLast());
            sliderControl.updateHistoryRange();
            refresh_map();
        }
        else if (e.keyCode == '39') {
            // right arrow
            timeRangeSlider.setValue(sliderControl.incrementLast());
            sliderControl.updateHistoryRange();
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
        closeNexusDetails(); closeEditNexusDetails();

        if (mode == 'inspect') {
            inspectLocation(clickEvent);
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
    });
}

function TimeRangeControl (div, map, rangeStart, rangeEnd) {
    var control = this;

    // Set the center property upon construction
    control.rangeStart_ = rangeStart;
    control.rangeEnd_ = rangeEnd;
}

/**
 * Define properties to hold the range states
 * @private
 */
TimeRangeControl.prototype.rangeStart_ = null;
TimeRangeControl.prototype.rangeEnd_ = null;
TimeRangeControl.prototype.last_ = 'current';
TimeRangeControl.prototype.timeZoomStack_ = [];
TimeRangeControl.prototype.sliderLabels = [];
TimeRangeControl.prototype.sliderDates = [];

TimeRangeControl.prototype.getRangeOrigin = function() {
    return this.rangeStart_;
};
TimeRangeControl.prototype.getRangeCurrent = function() {
    return this.rangeEnd_;
};
TimeRangeControl.prototype.setRangeOrigin = function(val) {
    this.rangeStart_ = val;
};
TimeRangeControl.prototype.setRangeCurrent = function(val) {
    this.rangeEnd_ = val;
};
TimeRangeControl.prototype.updateHistoryRange = function () {
    var vals = timeRangeSlider.getValue();
    sliderControl.setSliderLabels(vals);

    var originOrig = document.getElementById('sliderOriginLabel').innerHTML;
    var originNew = sliderControl.sliderLabels[0];
    if (originOrig != originNew) this.last_ = 'origin';
    document.getElementById('sliderOriginLabel').innerHTML = originNew;

    var currentOrig = document.getElementById('sliderCurrentLabel').innerHTML;
    var currentNew = sliderControl.sliderLabels[1];
    if (currentOrig != currentNew) this.last_ = 'current';
    document.getElementById('sliderCurrentLabel').innerHTML = currentNew;

    this.updateRangeLabels(sliderControl.sliderDates[0], sliderControl.sliderDates[1]);
};

function refresh_map () {
    var bounds = map.getBounds();
    return Api.find_nexuses(sliderControl.sliderDates[0],
                            sliderControl.sliderDates[1],
                            bounds.getNorthEast().lat(),
                            bounds.getSouthWest().lat(),
                            bounds.getNorthEast().lng(),
                            bounds.getSouthWest().lng(),
                            update_map);
}

TimeRangeControl.prototype.updateRangeLabels = function (start, end) {
    var startDate = document.getElementById('startDate');
    if (startDate != null) startDate.value = start;

    var endDate = document.getElementById('endDate');
    if (endDate != null) endDate.value = end;
};
TimeRangeControl.prototype.getYears = function(val) {
    var year = parseInt(val); // coerce to integer
    if (year == 0) year += 1; // skip year zero
    return (year < 0) ? localizer.numberFormatter(-1 * year) + " BCE " : localizer.numberFormatter(year) + " CE";
};
TimeRangeControl.prototype.getYearsDate = function(val) {
    var year = parseInt(val); // coerce to integer
    if (year == 0) year += 1; // skip year zero
    return year;
};
function rangeDateObj(val, year) {
    var yearFraction = parseFloat(val) - parseInt(year);
    var thisYearMillis = Date.UTC(year, 0);
    var nextYearMillis = Date.UTC(year + 1, 0);
    var nowMillis = thisYearMillis + ((nextYearMillis - thisYearMillis) * yearFraction);
    return new Date(nowMillis);
}
TimeRangeControl.prototype.getMonth = function(val) {
    var year = parseInt(val); // coerce to integer
    if (year == 0) year += 1; // skip year zero, does not exist
    var thisDate = rangeDateObj(val, year);
    var month = thisDate.getMonth()+1;
    return (year < 0) ? localizer.numberFormatter(-1*year) + "-" + month + " BCE" : localizer.numberFormatter(year) + "-" + month + " CE";
};
TimeRangeControl.prototype.getMonthDate = function(val) {
    var year = parseInt(val); // coerce to integer
    if (year == 0) year += 1; // skip year zero, does not exist
    var thisDate = rangeDateObj(val, year);
    var month = thisDate.getMonth()+1;
    return this.getYearsDate(val) + "-" + month;
};
TimeRangeControl.prototype.getDay = function(val) {
    var year = parseInt(val); // coerce to integer
    if (year == 0) year += 1; // skip year zero, does not exist
    var thisDate = rangeDateObj(val, year);
    var day = thisDate.getDate();
    return this.getMonthDate(val) + "-" + day;
};
TimeRangeControl.prototype.getDayDate = function(val) {
    var year = parseInt(val); // coerce to integer
    if (year == 0) year += 1; // skip year zero, does not exist
    var thisDate = rangeDateObj(val, year);
    var month = thisDate.getMonth()+1;
    var day = thisDate.getDate();
    return (year < 0) ? (-1*year) + "-" + month + "-" + day : (year) + "-" + month + "-" + day;
};
TimeRangeControl.prototype.getRangePoint = function(val) {
    var currentRange = (1.0*this.rangeEnd_) - this.rangeStart_;
    return (1.0*this.rangeStart_) + (((1.0*val) / MAX_SLIDER) * currentRange);
};
TimeRangeControl.prototype.getValueForRangePoint = function(val) {
    var currentRange = (1.0*this.rangeEnd_) - this.rangeStart_;
    var number = parseFloat(parseFloat(parseFloat(val) - this.rangeStart_) / currentRange) * MAX_SLIDER;
    //console.log("zoomToDates: "+val+" -> "+number+" (rangestart="+this.rangeStart_+", rangeend="+this.rangeEnd_+", currentRange="+currentRange+")");
    return number;
};
TimeRangeControl.prototype.setSliderLabels = function(vals) {
    this.sliderLabels = [ this.label(vals[0]), this.label(vals[1]) ];
    this.sliderDates = [ this.date(vals[0]), this.date(vals[1]) ];
}
TimeRangeControl.prototype.label = function(val) {
    if (localizer === undefined) return "";
    var rangePoint = this.getRangePoint(val);
    //console.log("rangePoint("+val+")="+rangePoint);
    var currentRange = (1.0*this.rangeEnd_) - this.rangeStart_;
    if (currentRange > 400 || rangePoint < -1000) {
        return this.getYears(rangePoint);
    } else if (currentRange > 100) {
        return this.getMonth(rangePoint);
    } else {
        return this.getDay(rangePoint);
    }
};
TimeRangeControl.prototype.date = function(val) {
    if (localizer === undefined) return "";
    var rangePoint = this.getRangePoint(val);
    var currentRange = (1.0*this.rangeEnd_) - this.rangeStart_;
    if (currentRange > 400 || rangePoint < -1000) {
        return this.getYearsDate(rangePoint);
    } else if (currentRange > 100) {
        return this.getMonthDate(rangePoint);
    } else {
        return this.getDayDate(rangePoint);
    }
};
TimeRangeControl.prototype.incrementLast = function() {
    var vals = timeRangeSlider.getValue();
    return (this.last_ == 'origin') ? [ vals[0]+1, vals[1] ] : [ vals[0], vals[1]+1 ];
};
TimeRangeControl.prototype.decrementLast = function() {
    var vals = timeRangeSlider.getValue();
    return (this.last_ == 'origin') ? [ vals[0]-1, vals[1] ] : [ vals[0], vals[1]-1 ];
};

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

function timelineZoomIn () {
    zoomIn(timeRangeSlider.getValue());
}

function zoomToDates (date1, date2) {
    zoomIn([ sliderControl.getValueForRangePoint(date1), sliderControl.getValueForRangePoint(date2) ]);
}

function zoomIn (vals) {
    if (vals[0] == 0 && vals[1] == MAX_SLIDER) return;

    sliderControl.timeZoomStack_.push([sliderControl.getRangeOrigin(), sliderControl.getRangeCurrent()]);

    var newStart = sliderControl.getRangePoint(vals[0]);
    var newEnd = sliderControl.getRangePoint(vals[1]);

    sliderControl.setRangeOrigin(newStart);
    sliderControl.setRangeCurrent(newEnd);

    if (sliderControl.timeZoomStack_.length > 0) {
        document.getElementById('btnZoomOut').disabled = false;
    }

    timeRangeSlider.setValue([0, MAX_SLIDER]);
    sliderControl.updateHistoryRange();
}

function timelineZoomOut () {
    if (sliderControl.timeZoomStack_.length > 0) {
        var vals = sliderControl.timeZoomStack_.pop();
        sliderControl.setRangeOrigin(vals[0]);
        sliderControl.setRangeCurrent(vals[1]);
    }
    if (sliderControl.timeZoomStack_.length == 0) {
        document.getElementById('btnZoomOut').disabled = true;
    }
    timeRangeSlider.setValue([0, MAX_SLIDER]);
    sliderControl.updateHistoryRange();
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

function inspectLocation (clickEvent) {
    console.log('inspecting: ' + clickEvent);
}

var active_markers = [];

function newMarkerListener(nexusSummary) {
    return function() { openNexusDetails(nexusSummary, 0); }
}

function update_map (data) {
    if (data && data.results && data.results instanceof Array) {

        // clear existing markers
        for (var i = 0; i < active_markers.length; i++) {
            active_markers[i].setMap(null);
        }
        active_markers = [];

        for (var i = 0; i < data.results.length; i++) {
            var result = data.results[i];
            //console.log("update_map: result[" + i + "] is: " + result);
            if (typeof result.primary != "undefined" && typeof result.primary.geo != "undefined" && result.primary.geo != null && result.primary.geo.type == "Point") {
                markerImage = get_marker_image(result.primary);
                var marker = new google.maps.Marker({
                    position: {lat: result.primary.geo.coordinates[1], lng: result.primary.geo.coordinates[0]},
                    title: result.primary.name,
                    icon: markerImage,
                    map: map
                });

                var nexus = result.primary;
                marker.addListener('click', newMarkerListener(result));

                active_markers.push(marker);
            }
        }
    }
}

var marker_colors = ['blue', 'brown', 'darkgreen', 'green', 'orange', 'paleblue', 'pink', 'purple', 'red', 'yellow'];
function get_marker_image (nexus) {
    if (typeof nexus.nexusType != "undefined" && nexus.nexusType != null) {
        var color = marker_colors[ Math.abs((nexus.nexusType.hashCode() + 6) % marker_colors.length) ];
        var initial = nexus.nexusType.charAt(0).toUpperCase();
        return '/markers/' + color + '_Marker' + initial + '.png';
    }
    return null;
}
