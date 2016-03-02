
function closeSearchOptions () { closeForm('searchOptionsContainer'); }

MARKER_COLORS = ['blue','brown','darkgreen','green','orange','paleblue','pink','purple','red','yellow'];

function initSearchForm () {
    addSearchRow(false);
    var picker = $('#markerColorPickerContainer');
    for (var i=0; i<MARKER_COLORS.length; i++) {
        picker.append('<img src="markers/'+MARKER_COLORS[i]+'_Marker_blank.png"/>');
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
}

function openMarkerColorPicker (e) {

    var activeColor = getColorFromImage(img.src);
    console.log('clicked on a '+activeColor+' marker');
    var picker = $('#markerColorPickerContainer');
    var pos = $(img).position();
    var width = $(img).outerWidth();
    picker.css({
        zIndex: 1,
        position: "absolute",
        top: pos.top + "px",
        left: (pos.left + width) + "px"
    });
}

function closeMarkerColorPicker () {
    var picker = $('#markerColorPickerContainer');
    picker.css('z-index', -1);
}

// strip leading prefix before first underscore, the remainder is the id for the row
function searchRowIdFromOtherId(id) { return id.substring(id.indexOf('_') + 1); }

function buildSearchRow (color, includeRemoveIcon) {

    var id = guid();
    var row = $('<tr id="row_'+id+'"></tr>');

    var markerImage = $('<img id="marker_'+ id+'" class="searchBox_markerImage" src="markers/'+color+'_Marker_blank.png"/>');
    var markerCell = $('<td id="markerClickTarget_'+id+'" align="center" valign="middle"></td>').append(markerImage);

    markerImage.click(function (e) {
        var image = $('#'+e.target.id);
        var parent = image.parent();
        var childImages = parent.find('.searchBox_markerImage');
        var src = childImages[0].src;
        color = getColorFromImage(src);
        console.log('marker clicked, target=' + e.target.id + ', color='+color);
    });
    row.append(markerCell);

    var queryTextField = $('<input id="text_'+id+'" class="searchBox_query" type="text"/>');
    queryTextField.keyup(function (e) {
        var id = searchRowIdFromOtherId(e.target.id);

        var markerId = 'marker_' + id;
        var markerImage = $('#'+markerId);

        var textId = 'text_' + id;
        var searchBox = $('#'+textId);

        var currentSrc = markerImage.attr('src');
        var color = getColorFromImage(currentSrc);

        var searchTerms = searchBox.val();
        var initial = '_blank';
        if (typeof searchTerms != "undefined" && searchTerms != null && searchTerms.length > 0) {
            for (var i=0; i<searchTerms.length; i++) {
                if (searchTerms.charAt(i).match(/[a-z]/i)) {
                    initial = searchTerms.charAt(i).toUpperCase();
                    break;
                }
            }
        }
        var newMarkerImage = 'markers/'+color+'_Marker'+initial+'.png';

        // only replace src if it is different
        if (currentSrc.indexOf(newMarkerImage) == -1) {
            markerImage.attr('src', newMarkerImage);
        }
    });

    var queryCell = $('<td align="center"></td>').append(queryTextField);
    row.append(queryCell);

    row.append('<td align="center"><button id="button_'+id+'" class="searchBox_button" onclick="doSearch(this); return false;" type="text">search</button></td>');

    if (includeRemoveIcon) {
        var removeRowIcon = $('<img id="removeMarkerIcon_'+ id+'" class="removeMarkerIcon" src="iconic/png/x.png"/>');
        removeRowIcon.click(function (e) {
            var rowId = 'row_' + searchRowIdFromOtherId(e.target.id);
            $('#'+rowId).remove();
            // todo: remove markers on map associated with this search row
        });
        row.append('<td align="center" valign="bottom"></td>').append(removeRowIcon);
    }

    return row;
}

