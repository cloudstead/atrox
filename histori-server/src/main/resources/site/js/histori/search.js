
function closeSearchOptions () { closeForm('searchOptionsContainer'); }

MARKER_COLORS = ['blue','brown','darkgreen','green','orange','paleblue','pink','purple','red','yellow'];

function initSearchForm () {
    addSearchRow();
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

function addSearchRow () {
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

    var row = buildSearchRow(color);
    var tbody = $('#searchBoxesTableBody');
    tbody.append(row);
}

function openMarkerColorPicker (e) {

    var jqImage = $('#'+img.id);
    var searchBox = $('#text_'+img.id);
    var searchTerms = searchBox.val();
    var initial = '_blank';
    if (typeof searchTerms != "undefined" && searchTerms != null && searchTerms.length > 0 && searchTerms.charAt(0).match(/[a-z]/i)) {
        initial = searchTerms.charAt(0).toUpperCase();
    }

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

function buildSearchRow (color) {
    var row = $('<tr>');
    var id = guid();
    var removeRowIcon = $('<img id="removeMarkerIcon_'+ id+'" class="removeMarkerIcon" src="iconic/png/x.png"/>');
    var markerImage = $('<img id="marker_'+ id+'" class="searchBox_markerImage" src="markers/'+color+'_Marker_blank.png"/>');
    var markerCell = $('<td id="markerClickTarget_'+id+'" align="center" valign="middle"></td>').append(markerImage).append(removeRowIcon);
    markerImage.click(function (e) {
        var image = $('#'+e.target.id);
        var parent = image.parent();
        var childImages = parent.find('.searchBox_markerImage');
        var src = childImages[0].src;
        color = getColorFromImage(src);
        console.log('marker clicked, target=' + e.target.id + ', color='+color);
    });
    row.append(markerCell);
    row.append('<td align="center"><input id="text_'+id+'" class="searchBox_query" type="text"/></td>');
    row.append('<td align="center"><button id="button_'+id+'" class="searchBox_button" onclick="doSearch(this); return false;" type="text">search</button></td>');
    return row;
}

