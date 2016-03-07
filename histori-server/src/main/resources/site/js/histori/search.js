// initialize
$(function() {

    addSearchRow();

    var picker = $('#markerColorPickerContainer');
    for (var i=0; i<MARKER_COLORS.length; i++) {
        var templateImage = $('<img src="markers/'+MARKER_COLORS[i]+'_Marker_blank.png"/>');
        var color = MARKER_COLORS[i];
        templateImage.click(colorPickerClickHandler(color));
        picker.append(templateImage);
    }

    $('#map').bind('keyup', function(e) {
        if (e.keyCode == 83) { // 's'
            var currentZ = $('#searchOptionsContainer').css('zIndex');
            if (currentZ > 0) {
                closeSearchOptions();
            } else {
                showSearchOptions();
            }
        }
    });
});

MARKER_COLORS = ['red','orange','yellow','green','darkgreen','paleblue','blue','purple','brown','pink'];
MAX_SEARCH_BOXES = 5;

function refresh_map () {
    $('.searchRow').each(function (index) {
        var row = $(this);
        var id = searchRowIdFromOtherId(row[0].id);
        doSearch(id);
    });
}

function showSearchOptions () { showForm('searchOptionsContainer', jQuery.fn.centerTop); }
function closeSearchOptions () { closeForm('searchOptionsContainer'); }

function rowSearchBox(id) { return $('#text_' + id); }
function rowMarkerImage(id) { return $('#marker_' + id); }
function rowMarkerImageSrc(id) {
    var marker = $('#marker_' + id);
    if (marker.length == 0) {
        console.log('no marker image found for id='+id);
        return null;
    }
    return marker[0].src;
}
function rowLoadingImage(id) { return $('#loading_' + id); }
function rowRemoveMarkerIcon(id) { return $('#removeMarkerIcon_' + id); }

function showLoadingSpinner (id) { rowLoadingImage(id).css('visibility', 'visible'); }
function hideLoadingSpinner (id) { rowLoadingImage(id).css('visibility', 'hidden'); }

function newExactSearch (term) { return newSearch('e:"'+term+'"'); }

function newSearch (term) {
    var rows = $('.searchRow');
    var row = null;
    // If this is the first search, and the first search is blank, replace it
    if (rows.length == 1) {
        if (rowSearchBox(searchRowIdFromOtherId(rows[0].id)).val().length == 0) {
            row = rows[0];
        }
    }
    // Try to add a new search row
    if (row == null) {
        row = addSearchRow();
        if (row != null) row = row[0];
    }
    // If adding a new row failed, use the last search row
    if (row == null) {
        row = rows[rows.length-1];
    }
    var id = searchRowIdFromOtherId(row.id);
    var searchBox = rowSearchBox(id);
    searchBox.val(term);
    doSearch(id);
    updateMarkerInitialLetter(id);
    closeNexusDetails();
    showSearchOptions();
    return row;
}

function newSearchLink (term) {
    // todo: cannot figure out how to get proper link styling using CSS, forced to use style attribute here
    return '<a href="." class="searchLink" style="text-decoration: none; color: black" onclick="newExactSearch(\''+term.escape()+'\'); return false;">'+term+'</a>'
}

function colorPickerClickHandler (color) {
    return function (e) {
        var id = $('#colorPickerRowId').val();

        var markerImage = rowMarkerImage(id);
        var searchBox = rowSearchBox(id);
        var initial = getInitialFromSearchBox(searchBox);

        var currentSrc = markerImage.attr('src');
        var newMarkerImage = 'markers/'+color+'_Marker'+initial+'.png';

        // only replace src if it is different
        if (currentSrc.indexOf(newMarkerImage) == -1) {
            markerImage.attr('src', newMarkerImage);
        }
        update_markers(id, newMarkerImage);
        closeMarkerColorPicker();
    }
}

function doSearch (id) {
    remove_markers(id);
    var bounds = map.getBounds();
    var searchBox = rowSearchBox(id);
    var query = searchBox.val();
    var search = {
        start: slider.canonical_start(),
        end: slider.canonical_end(),
        north: bounds.getNorthEast().lat(),
        south: bounds.getSouthWest().lat(),
        east: bounds.getNorthEast().lng(),
        west: bounds.getSouthWest().lng(),
        query: query
    };
    Histori.save_session_state();
    Api.find_nexuses(id, search, function () {showLoadingSpinner(id)}, update_map(id));
}

function getColorFromImage (src) {
    var pos = src.indexOf('markers/');
    if (pos == -1) {
        console.log('invalid src: '+src);
        return null;
    }
    var color = src.substring(pos+'markers/'.length);
    pos = color.indexOf('_');
    if (pos == -1) {
        console.log('invalid src: '+src);
        return null;
    }
    return color.substring(0, pos);
}

function pickUnusedColor() {
    var used_colors = [];
    $('.searchBox_markerImage').each(function (index) {
        var src = this.src;
        used_colors.push(getColorFromImage(src));
    });

    // find the first unused color
    var color = 'red';
    for (var i = 0; i < MARKER_COLORS.length; i++) {
        if ($.inArray(MARKER_COLORS[i], used_colors) < 0) {
            color = MARKER_COLORS[i];
            break;
        }
    }
    return color;
}

function addSearchRow () {

    if ($('.searchRow').length >= MAX_SEARCH_BOXES) {
        $('#btn_addSearchRow').attr('disabled', true);
        return null;
    }
    var color = pickUnusedColor();
    var row = buildSearchRow(color);
    var tbody = $('#searchBoxesTableBody');
    tbody.append(row);
    $('#searchOptionsContainer').centerTop(20);

    updateRemoveMarkerIcons();

    return row;
}

function updateRemoveMarkerIcons() {
    var rows = $('.searchRow');
    if (rows.length >= MAX_SEARCH_BOXES) {
        $('#btn_addSearchRow').attr('disabled', true);
        $('.removeMarkerIcon').css('visibility', 'visible');

    } else if (rows.length == 1) {
        // last search box -- hide 'remove' button
        var id = searchRowIdFromOtherId(rows[0].id);
        var removeIcon = rowRemoveMarkerIcon(id);
        removeIcon.css('visibility', 'hidden');

    } else {
        // all remove icons are visible
        $('.removeMarkerIcon').css('visibility', 'visible');
    }
}

function openMarkerColorPicker (e, id) {

    var markerImage = rowMarkerImage(id);

    var currentSrc = markerImage.attr('src');
    var currentColor = getColorFromImage(currentSrc);

    console.log('clicked on a '+currentColor+' marker');
    $('#colorPickerRowId').val(id); // store id in picker
    var picker = $('#markerColorPickerContainer');
    var width = 0;
    picker.css({
        zIndex: 1,
        position: "absolute",
        top: e.pageY + "px",
        left: (e.pageX + width) + "px"
    });
}

function closeMarkerColorPicker () {
    var picker = $('#markerColorPickerContainer');
    picker.css('z-index', -1);
}

// strip leading prefix before first underscore, the remainder is the id for the row
function searchRowIdFromOtherId(id) { return id.substring(id.indexOf('_') + 1); }

function getInitialFromSearchBox(searchBox) {
    var query = searchBox.val();
    var initial = '_blank';
    if (typeof query != "undefined" && query != null && query.length > 0) {
        var spacePos = query.indexOf(' ');
        var colonPos = query.indexOf(':');
        if (colonPos != -1 && (spacePos == -1 || colonPos < spacePos)) {
            query = query.substring(colonPos);
        }
        for (var i = 0; i < query.length; i++) {
            if (query.charAt(i).match(/[a-z]/i)) {
                initial = query.charAt(i).toUpperCase();
                break;
            }
        }
    }
    return initial;
}

function updateMarkerInitialLetter(id) {
    var markerImage = rowMarkerImage(id);
    var searchBox = rowSearchBox(id);

    var currentSrc = markerImage.attr('src');
    var color = getColorFromImage(currentSrc);
    var initial = getInitialFromSearchBox(searchBox);
    var newMarkerImage = 'markers/' + color + '_Marker' + initial + '.png';

    // only replace src if it is different
    if (currentSrc.indexOf(newMarkerImage) == -1) {
        markerImage.attr('src', newMarkerImage);
    }
    update_markers(id, newMarkerImage);
}

function searchButtonClickHandler (id) {
    var f = function (e) {
        console.log('searchButtonClickHandler: handling for id='+id);
        doSearch(id);
    };
    console.log('searchButtonClickHandler('+id+') returning '+f);
    return f;
}

function buildSearchRow (color) {

    var id = guid();
    var row = $('<tr class="searchRow" id="row_'+id+'"></tr>');

    var markerImage = $('<img id="marker_'+ id+'" class="searchBox_markerImage" src="markers/'+color+'_Marker_blank.png"/>');
    var loadingImage = $('<img id="loading_'+ id+'" src="icons/spinner.gif" style="visibility: hidden"/>');
    var markerCell = $('<td id="markerClickTarget_'+id+'" align="center" valign="middle"></td>').append(markerImage).append(loadingImage);

    markerImage.click(function (e) {
        var id = searchRowIdFromOtherId(e.target.id);
        var src = rowMarkerImageSrc(id);
        var color = getColorFromImage(src);
        openMarkerColorPicker(e, id);
        console.log('marker clicked, target=' + e.target.id + ', color='+color);
    });
    row.append(markerCell);

    var queryTextField = $('<input id="text_'+id+'" class="searchBox_query" type="text"/>');
    queryTextField.keydown(function (e) {
        if (e.keyCode == 13) {
            console.log('keydown on '+ e.target.id+', preventing submission...');
            e.preventDefault();
            return false;
        }
    });
    queryTextField.keyup(function (e) {
        var id = searchRowIdFromOtherId(e.target.id);
        updateMarkerInitialLetter(id);
        if (e.keyCode == 13) {
            doSearch(id);
        }
    });
    queryTextField.on('paste', function () {
        setTimeout(function () { updateMarkerInitialLetter(id); }, 10);
    });

    var queryCell = $('<td align="center"></td>').append(queryTextField);
    row.append(queryCell);

    var searchButton = $('<button id="button_'+id+'" class="searchBox_button" type="text">search</button>');
    searchButton.click(searchButtonClickHandler(id));
    var searchButtonCell = $('<td align="center"></td>').append(searchButton);
    row.append(searchButtonCell);

    var removeRowIcon = $('<img id="removeMarkerIcon_'+ id+'" class="removeMarkerIcon" src="iconic/png/x.png"/>');
    removeRowIcon.click(function (e) {
        var id = searchRowIdFromOtherId(e.target.id);
        $('#row_'+id).remove();
        remove_markers(id);
        updateRemoveMarkerIcons();
        $('#searchOptionsContainer').centerTop(20);
    });
    row.append('<td align="center" valign="bottom"></td>').append(removeRowIcon);

    return row;
}
