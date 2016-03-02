
function closeSearchOptions () { closeForm('searchOptionsContainer'); }

MARKER_COLORS = ['red','orange','yellow','green','darkgreen','paleblue','blue','purple','brown','pink'];

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

function colorPickerClickHandler (color) {
    return function (e) {
        var id = $('#colorPickerRowId').val();

        var markerId = 'marker_' + id;
        var markerImage = $('#'+markerId);

        var textId = 'text_' + id;
        var searchBox = $('#' + textId);
        var initial = getInitialFromSearchBox(searchBox);

        var currentSrc = markerImage.attr('src');
        var newMarkerImage = 'markers/'+color+'_Marker'+initial+'.png';

        // only replace src if it is different
        if (currentSrc.indexOf(newMarkerImage) == -1) {
            markerImage.attr('src', newMarkerImage);
        }
        closeMarkerColorPicker();
    }
}

function doSearch (button) {
    console.log('doSearch: '+button);
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
    $('#searchOptionsContainer').center();

    if ($('.searchRow').length >= 5) {
        $('#btn_addSearchRow').attr('disabled', true);
    }
}

function openMarkerColorPicker (e, id) {

    var markerId = 'marker_' + id;
    var markerImage = $('#'+markerId);

    var textId = 'text_' + id;

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
    var markerId = 'marker_' + id;
    var markerImage = $('#' + markerId);

    var textId = 'text_' + id;
    var searchBox = $('#' + textId);

    var currentSrc = markerImage.attr('src');
    var color = getColorFromImage(currentSrc);
    var initial = getInitialFromSearchBox(searchBox);
    var newMarkerImage = 'markers/' + color + '_Marker' + initial + '.png';

    // only replace src if it is different
    if (currentSrc.indexOf(newMarkerImage) == -1) {
        markerImage.attr('src', newMarkerImage);
    }
}
function buildSearchRow (color, includeRemoveIcon) {

    var id = guid();
    var row = $('<tr class="searchRow" id="row_'+id+'"></tr>');

    var markerImage = $('<img id="marker_'+ id+'" class="searchBox_markerImage" src="markers/'+color+'_Marker_blank.png"/>');
    var markerCell = $('<td id="markerClickTarget_'+id+'" align="center" valign="middle"></td>').append(markerImage);

    markerImage.click(function (e) {
        var id = searchRowIdFromOtherId(e.target.id);
        var image = $('#'+e.target.id);
        var parent = image.parent();
        var childImages = parent.find('.searchBox_markerImage');
        var src = childImages[0].src;
        color = getColorFromImage(src);
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

    row.append('<td align="center"><button id="button_'+id+'" class="searchBox_button" onclick="doSearch(this); return false;" type="text">search</button></td>');

    if (includeRemoveIcon) {
        var removeRowIcon = $('<img id="removeMarkerIcon_'+ id+'" class="removeMarkerIcon" src="iconic/png/x.png"/>');
        removeRowIcon.click(function (e) {
            var rowId = 'row_' + searchRowIdFromOtherId(e.target.id);
            $('#'+rowId).remove();

            if ($('.searchRow').length < 5) {
                $('#btn_addSearchRow').attr('disabled', false);
            }
            $('#searchOptionsContainer').center();

            // todo: remove markers on map associated with this search row
        });
        row.append('<td align="center" valign="bottom"></td>').append(removeRowIcon);
    }

    return row;
}
