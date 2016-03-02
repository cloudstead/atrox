
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
