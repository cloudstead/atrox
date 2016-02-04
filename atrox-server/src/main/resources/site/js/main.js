MAX_SLIDER = 10000.0;

var map;
var mode = 'inspect';
var addRegionWindow = null;
var isClosed = false;
var poly = null;
var markers = [];
var timeRangeSlider;
var sliderControl;

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
    // check to see if we are logged in --
    var authMessageSlot = document.getElementById('authMessageSlot');
    if (get_token() == NO_TOKEN || ((typeof Atrox.account == 'undefined') || (typeof Atrox.account.email == 'undefined'))) {
        authMessageSlot.innerHTML
            = 'Currently anonymous. <a href="." onclick="showLoginForm(); return false;">Sign in</a> or '
            + '<a href="." onclick="showRegistrationForm(); return false;">Sign up</a>';
    } else {
        authMessageSlot.innerHTML
            = 'Currently logged in as '+Atrox.account.email+' <a href="." onclick="Atrox.logout(); return false;">Log out</a>';
    }
};

function showLoginForm () {
    var loginForm = document.getElementById('loginForm');
    var authFormSlot = document.getElementById('authFormSlot');
    authFormSlot.innerHTML = '';
    authFormSlot.appendChild(loginForm);
}

function showRegistrationForm () {
    var regForm = document.getElementById('regForm');
    var authFormSlot = document.getElementById('authFormSlot');
    authFormSlot.innerHTML = '';
    authFormSlot.appendChild(regForm);
}

function initMap () {

    var map = new google.maps.Map(document.getElementById('map'), {
        center: new google.maps.LatLng(20.0, 0.0),
        zoom: 2,
        mapTypeId: google.maps.MapTypeId.HYBRID,
        scaleControl: true
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
    document.getElementById('sliderTable').style.width = sliderScreenSize+"px";
    document.getElementById('sliderPre').style.width = ((sliderScreenSize*0.125)/1)+"px";
    document.getElementById('sliderPost').style.width = ((sliderScreenSize*0.125)/1)+"px";
    document.getElementById('sliderCell').style.width = sliderScreenSize+"px";
    document.getElementById('sliderCell').style.textAlign = "center";
    document.getElementById('sliderOriginLabel').innerHTML = sliderControl.getSliderLabel(0);
    document.getElementById('sliderCurrentLabel').innerHTML = sliderControl.getSliderLabel(MAX_SLIDER);
    var listener = function(){
        var vals = timeRangeSlider.getValue();
        document.getElementById('sliderOriginLabel').innerHTML = sliderControl.getSliderLabel(vals[0]);
        document.getElementById('sliderCurrentLabel').innerHTML = sliderControl.getSliderLabel(vals[1]);
    };
    timeRangeSlider.attachEvent("onChange", function () { sliderControl.updateLabels(); } );
    document.body.onkeydown = function (e) {
        e = e || window.event;
        if (e.keyCode == '37') {
            // left arrow
            timeRangeSlider.setValue(sliderControl.decrementLast());
            sliderControl.updateLabels();
        }
        else if (e.keyCode == '39') {
            // right arrow
            timeRangeSlider.setValue(sliderControl.incrementLast());
            sliderControl.updateLabels();
        }
    };

    addRegionWindow = new google.maps.InfoWindow({ content: addRegionForm() });

    isClosed = false;

    google.maps.event.addListener(map, 'click', function (clickEvent) {
        if (mode == 'inspect') {
            inspectLocation(clickEvent);
            return;
        }
        if (mode == 'addEvent') {
            console.log('show addEvent dialog here');
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
TimeRangeControl.prototype.updateLabels = function () {
    var vals = timeRangeSlider.getValue();
    var originOrig = document.getElementById('sliderOriginLabel').innerHTML;
    var originNew = sliderControl.getSliderLabel(vals[0]);
    if (originOrig != originNew) this.last_ = 'origin';
    document.getElementById('sliderOriginLabel').innerHTML = originNew;

    var currentOrig = document.getElementById('sliderCurrentLabel').innerHTML;
    var currentNew = sliderControl.getSliderLabel(vals[1]);
    if (currentOrig != currentNew) this.last_ = 'current';
    document.getElementById('sliderCurrentLabel').innerHTML = currentNew;
};
TimeRangeControl.prototype.getYears = function(val) {
    var year = parseInt(val); // coerce to integer
    if (year == 0) year += 1; // skip year zero
    var val2 = (year < 0) ? localizer.numberFormatter(-1*year) + " BCE " : localizer.numberFormatter(year) + " CE";
    return val2;
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
    return (year < 0) ? month + "-" + localizer.numberFormatter(-1*year) + " BCE" : "" + month + "-" + localizer.numberFormatter(year) + " CE";
};
TimeRangeControl.prototype.getDay = function(val) {
    var year = parseInt(val); // coerce to integer
    if (year == 0) year += 1; // skip year zero, does not exist
    var thisDate = rangeDateObj(val, year);
    var month = thisDate.getMonth()+1;
    var day = thisDate.getDate();
    return (year < 0) ? day + "-" + month + "-" + localizer.numberFormatter(-1*year) + " BCE" : day + "-" + month + "-" + localizer.numberFormatter(year) + " CE";
};
TimeRangeControl.prototype.getRangePoint = function(val) {
    var currentRange = (1.0*this.rangeEnd_) - this.rangeStart_;
    return (1.0*this.rangeStart_) + (((1.0*val) / MAX_SLIDER) * currentRange);
};
TimeRangeControl.prototype.getSliderLabel = function(val) {
    if (localizer === undefined) return "";
    var rangePoint = this.getRangePoint(val);
    var currentRange = (1.0*this.rangeEnd_) - this.rangeStart_;
    if (currentRange > 400 || rangePoint < -1000) {
        return this.getYears(rangePoint);
    } else if (currentRange > 100) {
        return this.getMonth(rangePoint);
    } else {
        return this.getDay(rangePoint);
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

function setMode (button) {
    mode = button.value;
    if (button.id == 'btnInspect') {
        button.style['font-weight'] = "bold";
        document.getElementById('btnAddRegion').style['font-weight'] = "normal";
        document.getElementById('btnAddEvent').style['font-weight'] = "normal";
    } else if (button.id == 'btnAddRegion') {
        button.style['font-weight'] = "bold";
        document.getElementById('btnInspect').style['font-weight'] = "normal";
        document.getElementById('btnAddEvent').style['font-weight'] = "normal";
    } else {
        button.style['font-weight'] = "bold";
        document.getElementById('btnInspect').style['font-weight'] = "normal";
        document.getElementById('btnAddRegion').style['font-weight'] = "normal";
    }
}

function timelineZoomIn () {
    var vals = timeRangeSlider.getValue();
    if (vals[0] == 0 && vals[1] == MAX_SLIDER) return;
    sliderControl.timeZoomStack_.push([sliderControl.getRangeOrigin(), sliderControl.getRangeCurrent()]);
    sliderControl.setRangeOrigin(sliderControl.getRangePoint(vals[0]));
    sliderControl.setRangeCurrent(sliderControl.getRangePoint(vals[1]));
    if (sliderControl.timeZoomStack_.length > 0) {
        document.getElementById('btnZoomOut').disabled = false;
    }
    timeRangeSlider.setValue([0, MAX_SLIDER]);
    sliderControl.updateLabels();
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
    sliderControl.updateLabels();
}

function addRegion () {
    console.log('createAtrox!');
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
        'Event: <input type="text" name="eventName"/><br/>' +
        'Start: <input type="text" name="startDate"/><br/>' +
        'End: <input type="text" name="endDate"/><br/>' +
        'Ideologies: <input type="text" name="ideologies"/><br/>' +
        '<br/>' +
        '<em>Estimated number of people affected</em><br/>' +
        '<br/>' +
        'High estimate: <input type="text" name="deathHigh"/><br/>' +
        'Middle estimate: <input type="text" name="deathMid"/><br/>' +
        'Low estimate: <input type="text" name="deathLow"/><br/>' +
        'Citations: <input type="text" name="citations"/><br/>' +
        '<br/>' +
        '<button id="btnCreate" value="add" onclick="addRegion(this.form)">create</button><br/>'+
        '<button id="btnCancel" value="cancel" onclick="cancelAddRegion()">cancel</button><br/>'+
        '</form></p>'+
        '</div>'+
        '</div>';
}

function inspectLocation (clickEvent) {
    console.log('inspecting: ' + clickEvent);
}
