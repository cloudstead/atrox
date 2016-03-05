MAX_SLIDER = 100000;

MONTH_SHORT_NAMES = [null, 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

var slider = {
    // start/end are in years, with the decimal portion representing how far along in the year
    range: {
        start: null,
        end: null
    },

    // return raw values of start/end controls, as a value between 0 - MAX_SLIDER
    start_value: function () { return $('#timeSlider').slider( "values", 0 ); },
    end_value: function () { return $('#timeSlider').slider( "values", 1 ); },

    // move slider controls to the far ends, both left and right
    reset_controls: function () { $('#timeSlider').slider( "values", [ 0, MAX_SLIDER ] ); },

    range_size_in_years: function () { return this.range.end - this.range.start; },

    // given a slider value from 0 - MAX_SLIDER, return the date within the range as a decimal
    raw_date_for_slider_value: function (value) {
        var percent = parseFloat(value) / parseFloat(MAX_SLIDER);
        var start = parseFloat(this.range.start);
        var end = parseFloat(this.range.end);
        return start + (percent * (end - start));
    },

    // given a slider value from 0 - MAX_SLIDER, returns an object: {year: year, month: month, day: day}
    date_for_slider_value: function (value) {

        var raw = this.raw_date_for_slider_value(value);

        if (raw > 0 && raw < 1) {
            // there is no year zero CE
            raw += 1.0;
        } else if (raw < 0 && raw > -1) {
            // there is no year zero BCE
            raw -= 1.0;
        }

        // If we have a nearly round number, just return it as a year
        if (Math.abs(raw - Math.round(raw)) < 0.001) {
            return { year: Math.round(raw) };
        }

        // Otherwise the year is the integer portion
        var year = parseInt(raw);

        // The remainder is the time within the year
        var remainder = raw - year;

        // If the total range is more than 400 years, or the value is before 1000 BCE, just return the year
        if (this.range_size_in_years() > 400 || year < -1000) return { year: year };

        // Which month and day?
        var year1_millis = Date.UTC(year, 0);
        var year2_millis = Date.UTC(year + 1, 0);
        var date = new Date(parseInt(parseFloat(year2_millis - year1_millis) * remainder));

        var month = date.getMonth() + 1;

        // If the total range is more than 100 years, just return year-month
        if (this.range_size_in_years() > 100) return { year: year, month: month };

        // return year-month-day
        var day = date.getDate();
        return { year: year, month: month, day: day };
    },

    // given a ymd (such as that returned from date_for_slider_value), return a formatted string for the date
    label_for_date: function (ymd) {
        var suffix;
        var year = ymd.year;
        if (year < 0) {
            suffix = ' BCE'; year *= -1;
        } else {
            suffix = ' CE';
        }

        if (this.range_size_in_years() > 400 || year < -1000) return ''+year+suffix;

        var month = (typeof ymd.month == 'undefined' || ymd.month == null) ? 1 : ymd.month;

        if (this.range_size_in_years() > 100) return ''+year+'-'+MONTH_SHORT_NAMES[month]+suffix;

        var day = (typeof ymd.day == 'undefined' || ymd.day == null) ? 1 : ymd.day;
        return ''+year+'-'+MONTH_SHORT_NAMES[month]+'-'+day+suffix;
    },

    // given a slider value from 0 - MAX_SLIDER, return a formatted date string
    label_for_slider_value: function (val) {
        return this.label_for_date(this.date_for_slider_value(val));
    },

    // given a slider value from 0 - MAX_SLIDER, return a canonical date string (so we can send to server for searches)
    canonical_label_for_slider_value: function (val) {
        var ymd = this.date_for_slider_value(val);
        var canonical = ''+ymd.year;
        if (typeof ymd.month != 'undefined' && ymd.month != null) canonical += '-'+ymd.month;
        if (typeof ymd.day != 'undefined' && ymd.day != null) canonical += '-'+ymd.day;
        return canonical;
    },

    // used by search.js
    canonical_start: function () { return this.canonical_label_for_slider_value(this.start_value()); },
    canonical_end: function () { return this.canonical_label_for_slider_value(this.end_value()); },

    update_labels: function (values) {
        if (typeof values == 'undefined' || values == null) {
            values = [this.start_value(), this.end_value()];
        }
        $('#sliderStartLabel').html(this.label_for_slider_value(values[0]));
        $('#sliderEndLabel').html(this.label_for_slider_value(values[1]));
    },

    zoom_stack: [],

    // wherever the current slider controls are located, make that the new total slider range
    zoom_in: function () {
        var start = this.raw_date_for_slider_value(this.start_value());
        var end = this.raw_date_for_slider_value(this.end_value());

        // only do the zoom if at least one of the sliders was moved
        if (this.range.start == start && this.range.end == end) return;

        this.zoom_to(start, end);
        refresh_map();
    },

    zoom_to: function(start, end) {
        // push old values onto the stack, enable zoom-out button
        this.zoom_stack.push(this.range);
        $('#btnZoomOut').attr('disabled', false);

        // update range
        this.range = {start: start, end: end};

        // move sliders to ends of new range
        this.reset_controls();

        // update labels and re-run searches
        this.update_labels();
    },

    zoom_out: function () {

        // if there is nothing on the stack, we can't zoom out
        if (this.zoom_stack.length == 0) return;

        // pop the stack
        this.range = this.zoom_stack.pop();

        // disable zoom-out button if the stack is now empty
        if (this.zoom_stack.length == 0) $('#btnZoomOut').attr('disabled', true);

        // move sliders to ends of new range
        this.reset_controls();

        // update labels and re-run searches
        this.update_labels();
        refresh_map();
    },

    // map of searchbox_id -> list of markers on the timeline
    markers: {},

    // remove all markers for a particular searchbox
    remove_markers: function (searchbox_id) {
        if (typeof this.markers[searchbox_id] == "undefined" || this.markers[searchbox_id] == null) return;
        for (var i=0; i<this.markers[searchbox_id].length; i++) {
            this.markers[searchbox_id][i].remove();
        }
        this.markers[searchbox_id] = null;
    },

    raw_date_to_pixel_offset: function (raw, width) {
        var year_offset = parseFloat(raw) - parseFloat(this.range.start);
        var pct_offset = year_offset / parseFloat(this.range_size_in_years());
        var pixel_offset = parseInt(pct_offset * width);
        return pixel_offset;
    },

    add_marker: function (searchbox_id, start, end, title, image_src, click_handler) {

        if (typeof this.markers[searchbox_id] == "undefined" || this.markers[searchbox_id] == null) {
            this.markers[searchbox_id] = [];
        }

        var slider_element = $('#timeSlider');
        var width = slider_element.width();

        var start_pixel_offset = this.raw_date_to_pixel_offset(start, width);
        var end_pixel_offset = (end == null) ? null : this.raw_date_to_pixel_offset(end, width);
        var image_width = end_pixel_offset == null ? 12 : 12 + (end_pixel_offset - start_pixel_offset);

        var imageId = guid();
        // todo: add start/end dates to tooltip label
        var marker = $('<img class="timelineMarker" title="'+title.escape()+'" id="timeline_marker_'+imageId+'" height="20" width="'+image_width+'" src="'+image_src+'"/>');
        marker.click(click_handler)
        marker.css({
            position: 'absolute',
            top: -15,
            left: start_pixel_offset,
            zIndex: 2
        });
        this.markers[searchbox_id].push(marker);
        slider_element.append(marker);
    },

    update_markers: function (searchbox_id, image_src) {

        if (typeof this.markers[searchbox_id] == "undefined" || this.markers[searchbox_id] == null) return;

        for (var i=0; i<this.markers[searchbox_id].length; i++) {
            this.markers[searchbox_id][i].attr('src', image_src);
        }
    }
};

// initialize
$(function() {
    var timeSlider = $('#timeSlider');

    timeSlider.slider({
        range: true,
        min: 0,
        max: MAX_SLIDER,
        values: [ 0, MAX_SLIDER ],
        slide: function( event, ui ) {
            slider.update_labels(ui.values);
        }
    });

    var sliderContainer = $('#sliderContainer');
    sliderContainer.centerBottom(75);

    var today = new Date();
    var this_year = parseFloat(today.getFullYear()) + parseFloat(today.getMonth())/12.0;
    slider.range = {start: -10000, end: this_year};
    slider.zoom_to(-4000, this_year);
    slider.zoom_to(1500, this_year);
    slider.reset_controls();
    slider.update_labels();
});
