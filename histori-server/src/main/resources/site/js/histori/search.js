
MARKER_COLORS = ['red','orange','yellow','green','darkgreen','paleblue','blue','purple','brown','pink'];
MAX_SEARCH_BOXES = 5;

function refresh_map () {
    $('.searchRow').each(function (index) {
        var row = $(this);
        var id = searchRowIdFromOtherId(row[0].id);
        doSearch(id);
    });
}

function initSearchForm () {

    addSearchRow(false);

    var picker = $('#markerColorPickerContainer');
    for (var i=0; i<MARKER_COLORS.length; i++) {
        var templateImage = $('<img src="markers/'+MARKER_COLORS[i]+'_Marker_blank.png"/>');
        var color = MARKER_COLORS[i];
        templateImage.click(colorPickerClickHandler(color));
        picker.append(templateImage);
    }
}

function showSearchOptions () { showForm('searchOptionsContainer', jQuery.fn.centerTop); }
function closeSearchOptions () { closeForm('searchOptionsContainer'); }

function rowSearchBox(id) { return $('#text_' + id); }
function rowMarkerImage(id) { return $('#marker_' + id); }
function rowMarkerImageSrc(id) { return $('#marker_' + id)[0].src; }
function rowLoadingImage(id) { return $('#loading_' + id); }

function showLoadingSpinner (id) { rowLoadingImage(id).css('visibility', 'visible'); }
function hideLoadingSpinner (id) { rowLoadingImage(id).css('visibility', 'hidden'); }

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
    var bounds = map.getBounds();
    var searchBox = rowSearchBox(id);
    var query = searchBox.val();
    console.log('doSearch: '+id+': '+query);
    Api.find_nexuses(id,
        timeSlider.dates[0],
        timeSlider.dates[1],
        bounds.getNorthEast().lat(),
        bounds.getSouthWest().lat(),
        bounds.getNorthEast().lng(),
        bounds.getSouthWest().lng(),
        query,
        function () {showLoadingSpinner(id)},
        update_map(id));
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

function addSearchRow (includeRemoveIcon) {
    var used_colors = [];
    $('.searchBox_markerImage').each(function (index) {
        var src = this.src;
        used_colors.push(getColorFromImage(src));
    });

    // find the first unused color
    var color = 'red';
    for (var i=0; i<MARKER_COLORS.length; i++) {
        if ($.inArray(MARKER_COLORS[i], used_colors) < 0) {
            color = MARKER_COLORS[i];
            break;
        }
    }

    var row = buildSearchRow(color, includeRemoveIcon);
    var tbody = $('#searchBoxesTableBody');
    tbody.append(row);
    $('#searchOptionsContainer').centerTop(20);

    if ($('.searchRow').length >= MAX_SEARCH_BOXES) {
        $('#btn_addSearchRow').attr('disabled', true);
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
    var searchTerms = searchBox.val();
    var initial = '_blank';
    if (typeof searchTerms != "undefined" && searchTerms != null && searchTerms.length > 0) {
        for (var i = 0; i < searchTerms.length; i++) {
            if (searchTerms.charAt(i).match(/[a-z]/i)) {
                initial = searchTerms.charAt(i).toUpperCase();
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
function buildSearchRow (color, includeRemoveIcon) {

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
    queryTextField.keyup(function (e) {
        var id = searchRowIdFromOtherId(e.target.id);
        updateMarkerInitialLetter(id);
    });

    var queryCell = $('<td align="center"></td>').append(queryTextField);
    row.append(queryCell);

    var searchButton = $('<button id="button_'+id+'" class="searchBox_button" type="text">search</button>');
    searchButton.click(function (e) {
        doSearch(id);
    });
    var searchButtonCell = $('<td align="center"></td>').append(searchButton);
    row.append(searchButtonCell);

    if (includeRemoveIcon) {
        var removeRowIcon = $('<img id="removeMarkerIcon_'+ id+'" class="removeMarkerIcon" src="iconic/png/x.png"/>');
        removeRowIcon.click(function (e) {
            var rowId = 'row_' + searchRowIdFromOtherId(e.target.id);
            $('#'+rowId).remove();

            if ($('.searchRow').length < MAX_SEARCH_BOXES) {
                $('#btn_addSearchRow').attr('disabled', false);
            }
            $('#searchOptionsContainer').centerTop(20);

            // todo: remove markers on map associated with this search row
        });
        row.append('<td align="center" valign="bottom"></td>').append(removeRowIcon);
    }

    return row;
}
