
TAG_TYPES = ['event_type', 'world_actor', 'result', 'impact', 'person', 'event', 'citation', 'idea', 'meta'];
TAG_TYPE_NAMES = ['event types', 'world actors', 'results', 'impacts', 'persons', 'events', 'citations', 'ideas', 'meta'];
TAG_TYPE_NAMES_SINGULAR = ['event type', 'world actor', 'result', 'impact', 'person', 'event', 'citation', 'idea', 'meta'];

DECORATORS_BY_TAG_TYPE = {
    'event_type': ['see_also'],
    'event': ['relationship', 'see_also'],
    'world_actor': ['role', 'see_also'],
    'person': ['role', 'world_actor', 'see_also'],
    'result': ['role', 'world_actor', 'see_also'],
    'impact': ['world_actor', 'estimate', 'low_estimate', 'high_estimate', 'see_also'],
    'citation': ['excerpt', 'see_also'],
    'idea': ['see_also'],
    'meta': ['value'],
    'unknown': ['value']
};

function formatTimePoint(date) {
    if (typeof date == "undefined" || date == null) return "";
    var year = date.year;
    var eraMultiplier = (year < 0) ? -1 : 1;
    var suffix = (year < 0) ? ' BCE' : '';
    var month = (typeof date.month == "undefined" || date.month == null) ? "" : MONTHS[date.month];
    var day = (typeof date.day == "undefined" || date.day == null) ? "" : date.day;

    return (day + ' ' + month + ' ' + (eraMultiplier * year) + suffix).trim();
}

var markupConverter = new showdown.Converter();

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

function enableNexusEditButtons (enable) {
    if (enable) {
        $('.nexusEditButtons').css('visibility', 'visible');
        $('.nexusViewButtons').css('visibility', 'hidden');
    } else {
        $('.nexusEditButtons').css('visibility', 'hidden');
        $('.nexusViewButtons').css('visibility', 'visible');
    }
    var editButton = $('#btn_nexusEdit');
    if (isAnonymous()) {
        editButton.html('sign in to edit');
        editButton.off('click');
        editButton.on('click', function () { showLoginForm(); return false; });
    } else {
        editButton.html('edit');
        editButton.off('click');
        editButton.on('click', function () { startEditingNexus(); return false; });
    }
    var nexus = Histori.active_nexus;
    if (nexus != null && typeof nexus.dirty != "undefined" && nexus.dirty) {
        $('#btn_nexusSave').removeAttr('disabled');
    } else {
        $('#btn_nexusSave').attr('disabled', 'disabled');
    }
}

function getTagNameDivId (tag) { return 'nexusTag_' + tag.uuid; }
function getNexusTagDivId (tag) { return 'nexusTagDiv_' + tag.uuid; }

function openNexusDetails (uuid, tries) {
    if (activeForm == 'editNexus') {
        console.log('openNexusDetails: edit in progress, not opening new nexus');
        return;
    }
    closeForm();
    var container = $('#nexusDetailsContainer');
    var nexus = Api.nexusCache[uuid];
    Histori.active_nexus = nexus;

    if (typeof nexus == "undefined" || nexus == null) return;
    if (typeof nexus.tags != "undefined" && typeof nexus.tags.tags != "undefined" && is_array(nexus.tags.tags)) {
        nexus.tags = nexus.tags.tags;
    }
    enableNexusEditButtons(false);

    $('#nexusNameContainer').html(nexus.name);
    $('#nexusAuthorContainer').empty();
    Api.owner_name(nexus.owner, '#nexusAuthorContainer', "v" + nexus.version +" created by: ");
    displayTimeRange(nexus.timeRange);

    $('#nexusGeoContainer').html(display_bounds(nexus.bounds));

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

    var commentaryContainer = $('#nexusCommentaryContainer');
    commentaryContainer.empty();
    if (typeof nexus.markdown != "undefined") {
        var markdown = $('<p class="commentaryMarkdown">'+markupConverter.makeHtml(nexus.markdown.replaceAll('\&amp;nbsp;', '&nbsp;').replaceAll('\&amp;ndash;', '&ndash;')).replaceAll('<a ', '<a target="_blank" ')+'</p>');
        commentaryContainer.append(markdown);
    }

    var tagsContainer = $('#nexusTagsContainer');
    tagsContainer.empty();

    if (typeof nexus.incomplete != "undefined" && nexus.incomplete) {
        if (tries < 5) {
            window.setTimeout(function () {
                console.log('trying to find nexus, try #' + tries);
                findNexus(nexus, tries);
            }, tries * 2000);
            showLoadingMessage("Loading nexus...");

        } else {
            showLoadingMessage("Loading nexus failed, maybe try again later");
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
                    var tagDivId = getNexusTagDivId(tags[j]);
                    listOfTags += '<div id="'+tagDivId+'" class="nexusTag">';
                    listOfTags += nexusTagContent(tags[j], names, false);
                    listOfTags += "</div>";

                    var nexusTagId = getTagNameDivId(tags[j]);
                    names.push({tag: tags[j].tagName, id: nexusTagId});
                }
                tagRow.append($('<td class="tagCell">' + listOfTags + '</td>'));
            }
            Api.resolve_tags(names, update_tag_display_name);
        }
    }

    if (nexus.bounds.east > map.getBounds().getCenter().lng()) {
        container.css('left', '20px');
        container.css('right', '');
    } else {
        container.css('left', '');
        container.css('right', '20px');
    }

    container.css('zIndex', 1);
}

function findNexus(nexus, tries) {
    Api.find_nexus_exact(nexus.uuid, function (data) {
        console.log('find_nexus for try #' + tries + ' returned nexus, incomplete=' + data.incomplete);
        openNexusDetails(nexus.uuid, tries + 1);
    }, null);
}

function displayTimeRange(timeRange) {
    var rangeStart = $('.nexusRangeStart');
    var rangeHyphen = $('.nexusRangeHyphen');
    var rangeEnd = $('.nexusRangeEnd');
    rangeStart.empty();
    rangeHyphen.empty();
    rangeEnd.empty();
    rangeStart.html(formatTimePoint(timeRange.startPoint));
    if (typeof timeRange.endPoint != "undefined" && timeRange.endPoint != null && ((typeof timeRange.endPoint.year != "undefined") && timeRange.endPoint.year != null && timeRange.endPoint.year != 0)) {
        rangeHyphen.html(' - ');
        rangeEnd.html(formatTimePoint(timeRange.endPoint));
    }
}

var checkDirtyTimer = null;
function checkDirty () {
    if (checkDirtyTimer != null) window.clearTimeout(checkDirtyTimer);
    checkDirtyTimer = window.setTimeout('_checkDirty()', 600);
}

function _checkDirty () {
    var nexus = Histori.active_nexus;
    if (nexus == null) return;
    Histori.active_nexus.dirty = isDirty(nexus);
    enableNexusEditButtons(true);
}

function isSameDate (timePoint, dateField) {
    if (formatTimePoint(timePoint) == dateField.val()) {
        console.log('isSameDate: text unchanged, not calling server');
        return true;
    }
    var range = Api.parse_date(dateField.val());
    if (range == null) {
        display_error_field(dateField);
        console.log('isSameDate: server error, returning false');
        return false;

    } else if (range.startPoint && range.startPoint.instant) {
        clear_error_field(dateField);
        var instant = range.startPoint.instant;
        var isSame = instant == timePoint.instant;
        console.log('isSameDate: returning '+isSame);
        return isSame;
        // dateField.val(formatTimePoint(range.startPoint));

    } else {
        console.log('invalid date but not changing anything');
        return false;
    }
}

function isDirty (nexus) {
    var editNameField = $('#nedit_name');
    if (editNameField && editNameField[0] && nexus.name != editNameField.val()) {
        return true;
    }
    var startRangeField = $('#nedit_nexusRangeStart');
    if (startRangeField && startRangeField[0] && !isSameDate(nexus.timeRange.startPoint, startRangeField)) {
        return true;
    }
    var endRangeField = $('#nedit_nexusRangeEnd');
    if (endRangeField && endRangeField[0]
        // end is optional, so additionally we check if one side is empty/null/undefined and the other has a value
        && (nexus.timeRange.endPoint && !isSameDate(nexus.timeRange.endPoint, endRangeField))
            || ((typeof nexus.timeRange.endPoint == "undefined" || nexus.timeRange.endPoint == null)
                 && endRangeField.val().trim().length > 0)) {
        return true;
    }
    var visibilityField = $('#nedit_visibility option:selected');
    if (visibilityField && visibilityField[0] && nexus.visibility != visibilityField.val()) {
        return true;
    }
    var commentaryField= $('#nedit_markdown');
    if (commentaryField && commentaryField[0] && commentaryField.val() != $('#nedit_markdown_original').val()) {
        return true;
    }
    switch (nexus.geo.type) {
        case "Point":
            var latField = $('#nedit_lat');
            var lonField = $('#nedit_lon');
            if (latField && lonField && latField[0] && lonField[0]
                && (latField.val() != nexus.geo.coordinates[1] || lonField.val() != nexus.geo.coordinates[0])) {
                return true;
            }
            break;
    }

    return tagsDirty;
}

function jsFieldDirtyChecks () { return 'onkeyup="checkDirty()" onchange="checkDirty()" onblur="checkDirty()"'; }

function updateGeoControls (geoContainer, nexus, geoTypeSelect) {
    return function () { addGeoControlsForType(geoContainer, nexus, $('#nedit_geo_type').val(), geoTypeSelect); };
}

function addGeoControls (geoContainer, nexus) {
    var geoTypeSelect = $('<select id="nedit_geo_type"></select>');
    geoTypeSelect.append($('<option>point</option>'));
    geoTypeSelect.append($('<option>line</option>'));
    geoTypeSelect.append($('<option>polygon</option>'));
    geoTypeSelect.append($('<option>multi-polygon</option>'));
    geoTypeSelect.on('change', updateGeoControls(geoContainer, nexus, geoTypeSelect));
    geoContainer.append(geoTypeSelect);

    var type = nexus.geo.type.toLowerCase();
    geoTypeSelect.val(type);
    addGeoControlsForType(geoContainer, nexus, type, geoTypeSelect);
}

function resetGeo (nexus) {
    remove_editable_markers(nexus);
    switch (nexus.geo.type.toLowerCase()) {
        case "point":
            var markers = find_markers(nexus.uuid);
            if (!markers || !markers[0] || markers.length > 1) {
                console.log('no markers or more than one marker');
            } else {
                markers[0].setPosition(new google.maps.LatLng(nexus.geo.coordinates[1], nexus.geo.coordinates[0]));
            }
            break;
    }
}

function addGeoControlsForType(geoContainer, nexus, type, geoTypeSelect) {
    var geoSpecific = $('.geoSpecific');
    geoSpecific.remove();
    switch (type) {
        case "point":
            geoContainer.append($('<span class="geoSpecific"> Lat: </span>'));
            geoContainer.append($('<input class="editNexusField geoLatLonField geoSpecific" id="nedit_lat" type="text" size="7" ' + jsFieldDirtyChecks() + ' value="' + nexus.geo.coordinates[1] + '"/>'));
            geoContainer.append($('<span class="geoSpecific"> Lon: </span>'));
            geoContainer.append($('<input class="editNexusField geoLatLonField geoSpecific" id="nedit_lon" type="text" size="7" ' + jsFieldDirtyChecks() + ' value="' + nexus.geo.coordinates[0] + '"/>'));
            break;
        default:
            geoContainer.append($('<span class="geoSpecific"> not (yet) supported</span>'));
            break;
    }
    var geoLatLonFields = $('.geoLatLonField');
    geoLatLonFields.off('change');
    geoLatLonFields.on('change', function () {
        switch (type.toLowerCase()) {
            case "point":
                var markers = find_editable_markers(nexus.uuid);
                if (markers && markers[0] && markers.length > 0) {
                    for (var i=0; i<markers.length; i++) {
                        markers[i].setPosition(new google.maps.LatLng($('#nedit_lat').val(), $('#nedit_lon').val()));
                    }
                }
                break;
        }
    });
    enable_editable_markers(nexus, function (marker) {
        var position = marker.getPosition();
        switch (type.toLowerCase()) {
            case "point":
                $('#nedit_lat').val(position.lat());
                $('#nedit_lon').val(position.lng());
                checkDirty();
                break;
        }
    });
}

function getSchemaValueId (tag, index, schemaVal) {
    return getTagNameDivId(tag) + '_' + index + '_' + schemaVal.value.safeId();
}

function nexusTagContent (tag, names, editable) {
    var tagNameDivId = getTagNameDivId(tag);
    var tagName = editable ? tag.tagName : newSearchLink(tag.tagName);
    var html = '<div class="nexusTag_tagName" id="' + tagNameDivId + '">' + tagName + '</div>';
    if (editable) html += '<input type="hidden" class="nexusTag_tagType" value="'+tag.tagType+'"/>';
    if (typeof tag.values != "undefined" && is_array(tag.values)) {
        var prevField = '';
        var numValues = tag.values.length;
        for (var k = 0; k < numValues; k++) {
            var schemaVal = tag.values[k];
            var displayField;
            var schemaValueId = getSchemaValueId(tag, k, schemaVal);
            var schemaTypeIndex = $.inArray(schemaVal.field, TAG_TYPES);
            if (schemaTypeIndex != -1) {
                displayField = TAG_TYPE_NAMES[schemaTypeIndex];
                if (names)  names.push({tag: schemaVal.value, id: schemaValueId});
            } else {
                displayField = schemaVal.field;
            }
            if (numValues > 1 && prevField != displayField) {
                //listOfTags += "<div class='schema_field'>"+ displayField.safeId() + "</div>";
                prevField = displayField;
            }
            html += "<div id='" + schemaValueId + "' class='schema_value'>" + schemaVal.field.replace('_', ' ') + ": " + schemaVal.value.replace('_', ' ') + "</div>";
        }
    }
    return html;
}

var deltaNexus = null;
var tagsDirty = false;

function startEditingNexus () {
    activeForm = 'editNexus';
    if (isAnonymous()) {
        showLoginForm();
        return;
    }
    closeForm();
    var container = $('#nexusDetailsContainer');
    var nexus = deltaNexus = JSON.parse(JSON.stringify(Histori.active_nexus)); // make a copy
    editingTag = null; // reset tag editor
    tagsDirty = false;

    if (typeof nexus == "undefined" || nexus == null) return;
    enableNexusEditButtons(true);

    // create name fields
    var nameContainer = $('#nexusNameContainer');
    nameContainer.empty();
    nameContainer.append($('<span class="editNexusLabel">Name: </span>'));
    nameContainer.append($('<input class="editNexusField" id="nedit_name" type="text" name="name" value="'+nexus.name+'" '+jsFieldDirtyChecks()+'>'));

    // empty author tag
    var authorContainer = $('#nexusAuthorContainer');
    authorContainer.empty();

    // add visibility field
    var visibilityControl = $('<select id="nedit_visibility" '+jsFieldDirtyChecks+'></select>');
    visibilityControl.append($('<option value="everyone"'+(nexus.visibility == "everyone" ? "selected" : "")+'>everyone</option>'));
    visibilityControl.append($('<option value="owner"'+(nexus.visibility == "owner" ? "selected" : "")+'>just me</option>'));
    visibilityControl.append($('<option value="hidden"'+(nexus.visibility == "hidden" ? "selected" : "")+'>hidden</option>'));
    authorContainer.append($('<span class="editNexusLabel">Visibility: </span>'));
    authorContainer.append(visibilityControl);

    // create time range fields
    var timeRange = nexus.timeRange;
    var startField = $('.edit_nexusRangeStart');
    var endField = $('.edit_nexusRangeEnd');
    if (!startField.length) startField = timePointInputBox('nexusRangeStart', timeRange.startPoint);
    if (!endField.length) endField = timePointInputBox('nexusRangeEnd', timeRange.endPoint);
    var rangeStart = $('.nexusRangeStart');
    var rangeHyphen = $('.nexusRangeHyphen');
    var rangeEnd = $('.nexusRangeEnd');
    rangeStart.empty(); rangeHyphen.empty(); rangeEnd.empty();
    rangeStart.append($('<span class="editNexusLabel">Date(s): </span>'));
    rangeStart.append(startField);
    rangeHyphen.html(' to ');
    rangeEnd.append(endField);

    // create geo fields, make map markers movable
    var geoContainer = $('#nexusGeoContainer');
    geoContainer.empty();
    geoContainer.append($('<span>Location: </span>'));
    addGeoControls(geoContainer, nexus);

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

    var commentaryContainer = $('#nexusCommentaryContainer');
    commentaryContainer.empty();
    var commentaryField = $('<textarea rows=12 cols=100 id="nedit_markdown" '+jsFieldDirtyChecks()+'></textarea>');
    commentaryContainer.append(commentaryField);
    if (typeof nexus.markdown != "undefined" && nexus.markdown != null && nexus.markdown.length > 0) {
        commentaryField.html(nexus.markdown);
        var origMarkdown = $('<input type="hidden" id="nedit_markdown_original"/>');
        origMarkdown.val(commentaryField.val());
        commentaryContainer.append(origMarkdown);
    }

    var tagsContainer = $('#nexusTagsContainer');
    tagsContainer.empty();

    if (typeof nexus.tags != "undefined" && is_array(nexus.tags)) {
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
            var tags = tagsByType[tagType];
            var tagTypeName = TAG_TYPE_NAMES_SINGULAR[typeIndex];
            var tagRow = $('<tr class="tagTypeRow">');
            tagRow.append($('<td class="tagTypeCell">' + tagTypeName + '</td>'));
            tbody.append(tagRow);
            var listOfTags = "";
            if (typeof tags != "undefined") {
                for (var j = 0; j < tags.length; j++) {
                    var tagDivId = getNexusTagDivId(tags[j]);
                    listOfTags += '<div id="'+tagDivId+'" class="nexusTag" onclick="startEditingTag(\''+tags[j].uuid+'\'); return false;">';
                    listOfTags += nexusTagContent(tags[j], null, true);
                    listOfTags += "</div>";
                }
            }
            listOfTags += "<div id='addTag_"+tagType+"' class='addTagButtonCell'><button onclick='addTag(\""+tagType+"\"); return false;'><img src='iconic/png/plus.png'/></button></div>";
            tagRow.append($('<td class="tagCell">' + listOfTags + '</td>'));
        }
    }

    if (nexus.bounds.east > map.getBounds().getCenter().lng()) {
        container.css('left', '20px');
        container.css('right', '');
    } else {
        container.css('left', '');
        container.css('right', '20px');
    }

    container.css('zIndex', 1);
}

function addTag (tagType) {
    var addCell = $('#addTag_'+tagType);
    var newTag = {uuid: guid(), tagName: '', tagType: tagType, values: []};
    var tagDivId = getNexusTagDivId(newTag);
    var html = '<div id="'+tagDivId+'" class="nexusTag" onclick="startEditingTag(\''+newTag.uuid+'\'); return false;">';
    html += nexusTagContent(newTag, null, true);
    html += "</div>";
    $(html).insertBefore(addCell);
    if (typeof deltaNexus.tags == "undefined") deltaNexus.tags = [];
    deltaNexus.tags.push(newTag);
    startEditingTag(newTag.uuid);
}

var editingTag = null;

function findTag (tagUuid) {
    var nexus = deltaNexus;
    if (nexus == null) nexus = Histori.active_nexus;
    var tag = null;
    for (var i = 0; i < nexus.tags.length; i++) {
        if (nexus.tags[i].uuid == tagUuid) {
            tag = nexus.tags[i];
            break;
        }
    }
    return tag;
}

JUST_CANCELED = 'just-canceled';

function deleteIdFunc (id) { return function () { $('#'+id).remove(); } }

function getDecoratorRowId (schemaValueId) { return 'decorator_'+schemaValueId; }

function addDecoratorRow (tag, k, tbody) {
    if (typeof k == "undefined" || k == null) k = tag.values.length - 1; // undefined/null means use the most recently added decorator
    if (typeof tbody == "undefined") tbody = $('#decoratorTbody');

    var schemaVal = tag.values[k];
    var schemaValueId = getSchemaValueId(tag, k, schemaVal);
    var decoratorRowId = getDecoratorRowId(schemaValueId);

    var tableRow = $('<tr id="'+decoratorRowId+'"></tr>');
    tbody.append(tableRow);

    var displayField;
    var schemaTypeIndex = $.inArray(schemaVal.field, TAG_TYPES);
    if (schemaTypeIndex != -1) {
        displayField = TAG_TYPE_NAMES_SINGULAR[schemaTypeIndex];
    } else {
        displayField = schemaVal.field.replace('_', ' ');
    }
    tableRow.append($('<td>'+displayField+': </td>'));

    var displayVal = schemaVal.value.replace('_', ' ');
    var valCell = $('<td></td>');
    tableRow.append(valCell);

    var valInput = $('<input id="editTag_'+schemaValueId+'" class="editNexusField editNexusDecoratorField" type="text" value="'+displayVal+'"/>');
    var typeInput = $('<input id="editTag_'+schemaValueId+'_type" type="hidden" value="'+schemaVal.field+'"/>');
    valCell.append(valInput);
    valCell.append(typeInput);

    var delCell = $('<td></td>');
    var delButton = $('<button><img src="iconic/png/x.png"/></button>');
    delButton.on('click', deleteIdFunc(decoratorRowId));
    delCell.append(delButton);
    tableRow.append(delCell);
}

function addDecoratorFunc (tag) {
    return function () {
        if (typeof tag.values == "undefined") tag.values = [];
        var decoratorType = $('#editTag_newDecoratorType');
        var decoratorValue = $('#editTag_newDecoratorValue');
        tag.values.push({field: decoratorType.val(), value: decoratorValue.val()});
        addDecoratorRow(tag);
        decoratorValue.val('');
    }
}

function startEditingTag (tagUuid) {
    if (editingTag == tagUuid) return;
    console.log("START>>> TAGUUID= "+tagUuid+", editingTag="+editingTag);
    if (editingTag == JUST_CANCELED) {
        editingTag = null;
        return;
    }
    if (editingTag != null) commitEditingTag(editingTag);
    editingTag = tagUuid;
    tagsDirty = true;
    checkDirty();

    var tag = findTag(tagUuid);
    if (tag == null) {
        console.log('startEditingTag: tag not found');
        return;
    }

    // name field
    var tagNameDivId = getNexusTagDivId(tag);
    var tagDiv = $('#'+ tagNameDivId);
    var tagNameField = $('#' + getTagNameDivId(tag));
    if (!tagNameField || !tagNameField.length) {
        console.log('tag name field not found: '+JSON.stringify(tag));
        editingTag = null;
        return;
    }
    var tagName = tagNameField[0].innerHTML;
    tagDiv.empty();
    var nameField = $('<input type="text" id="editTag_name" value="'+tagName+'"/>');
    tagDiv.append($('<span>Name: </span>'));
    tagDiv.append(nameField);

    // delete button
    var deleteButton = $('<button><img src="iconic/png/x-2x.png"/></button>');
    deleteButton.on('click', function () {
        tagDiv.remove();
        editingTag = null;
    });
    var deleteSpan = $('<span class="deleteTagButton"></span>');
    deleteSpan.append(deleteButton);
    tagDiv.append(deleteSpan);

    // decorator table
    var decoratorTable = $('<table></table>');
    //var thead = $('<thead><tr><th>type</th><th>value</th></tr></thead>');
    var tbody = $('<tbody id="decoratorTbody"></tbody>');
    var tfoot = $('<tfoot></tfoot>');
    decoratorTable.append(tbody);

    if (typeof tag.values == "undefined" || !is_array(tag.values)) {
        tag.values = [];
    }
    var numValues = tag.values.length;
    for (var k = 0; k < numValues; k++) {
        addDecoratorRow(tag, k, tbody);
    }

    var addRow = $('<tr></tr>');

    var typeCell = $('<td></td>');
    var decoratorTypes = DECORATORS_BY_TAG_TYPE[tag.tagType];
    if (typeof decoratorTypes == "undefined" || decoratorTypes.length == 0) decoratorTypes = DECORATORS_BY_TAG_TYPE['unknown'];
    if (decoratorTypes.length == 1) {
        var typeControl = $('<input type="hidden" id="editTag_newDecoratorType" value="'+decoratorTypes[0]+'"/>');
        typeCell.append(typeControl);
        typeCell.append($('<span>'+decoratorTypes[0].replace('_', ' ')+'</span>'));
    } else {
        var typeSelect = $('<select id="editTag_newDecoratorType"></select>');
        for (var typeIndex = 0; typeIndex < decoratorTypes.length; typeIndex++) {
            typeSelect.append($('<option value="' + decoratorTypes[typeIndex] + '">' + decoratorTypes[typeIndex].replace('_', ' ') + '</option>'));
        }
        typeCell.append(typeSelect);
    }

    var addValCell = $('<td></td>');
    var addValInput = $('<input id="editTag_newDecoratorValue" type="text" class="editNexusField editNexusDecoratorField"/>');
    addValCell.append(addValInput);

    var addButtonCell = $('<td></td>');
    var addButton = $('<button><img src="iconic/png/plus.png"></button>');
    addButton.on('click', addDecoratorFunc(tag));
    addButtonCell.append(addButton);

    addRow.append(typeCell);
    addRow.append(addValCell);
    addRow.append(addButtonCell);
    tfoot.append(addRow);

    decoratorTable.append(tfoot);
    tagDiv.append(decoratorTable);
}

function commitEditingTag (tagUuid) {
    var tag = findTag(tagUuid);
    if (tag == null) {
        console.log('commitEditingTag: tag not found');
        return;
    }

    tag.tagName = $('#editTag_name').val();
    tag.values = [];

    $('.editNexusDecoratorField').each(function (index) {
        var typeField = $('#'+$(this)[0].id+'_type');
        if (typeField && typeField[0]) {
            var type = typeField.val();
            tag.values.push({field: type, value: $(this).val()});
        }
    });

    var tagDiv = $('#'+getNexusTagDivId(tag));
    tagDiv.empty();
    tagDiv.append(nexusTagContent(tag, null, true));

    editingTag = JUST_CANCELED;
    checkDirty();
}

function cancelNexusEdits () {
    if (checkDirtyTimer != null) window.clearTimeout(checkDirtyTimer);
    if (Histori.active_nexus) {
        Histori.active_nexus.dirty = false;
        resetGeo(Histori.active_nexus);
        activeForm = null; // allow nexuses to be opened again
        openNexusDetails(Histori.active_nexus.uuid, 0);
    }
}

function populateEditedNexus () {
    if (editingTag != null) commitEditingTag(editingTag);

    deltaNexus.name = $('#nedit_name').val();
    deltaNexus.visibility = $('#nedit_visibility option:selected').val();
    deltaNexus.timeRange = {
        startPoint: { inputString: $('#nedit_nexusRangeStart').val() },
        endPoint: { inputString: $('#nedit_nexusRangeEnd').val() }
    };
    switch ($('#nedit_geo_type').val()) {
        case "point":
            var lon = Number($('#nedit_lon').val());
            var lat = Number($('#nedit_lat').val());
            if (!isNaN(lon) && !isNaN(lat) && !(lon == 0 && lat == 0)) {
                deltaNexus.geo = {type: 'Point', coordinates: [lon, lat]};
            }
            break;
    }
    // deltaNexus.geoJson = JSON.stringify(deltaNexus.point);
    deltaNexus.markdown = $('#nedit_markdown').val();

    var tags = [];
    $('.nexusTag').each(function (index) {
        var tagName = $(this).find('.nexusTag_tagName')[0].innerHTML;
        var tagType = $(this).find('.nexusTag_tagType').val();
        var values = [];
        $(this).find('.schema_value').each(function (index) {
            var html = $(this)[0].innerHTML.trim();
            var colonPos = html.indexOf(':');
            if (colonPos == -1 || colonPos == html.length - 1) {
                console.log('invalid schema value: '+html);
                return;
            }
            var decoratorType = html.substring(0, colonPos).trim();
            var decoratorValue = html.substring(colonPos+1).trim();
            values.push({field: decoratorType, value: decoratorValue});
        });
        tags.push({tagName: tagName, tagType: tagType, values: values});
    });
    deltaNexus.tags = { tags: tags };

    return deltaNexus;
}

function commitNexusEdits () {
    if (Histori.active_nexus != null && isDirty(Histori.active_nexus)) {
        Api.edit_nexus(Histori.active_nexus, populateEditedNexus(), function (data) {
            data.tags = data.tags.tags; // re-adjust tags
            Histori.active_nexus.dirty = false;
            Histori.active_nexus = data;
            Histori.active_nexus.incomplete = false;
            resetGeo(Histori.active_nexus);
            tagsDirty = false;
            editingTag = null;
            // todo: should we update the nexuses that are cached in main.js?
            if (checkDirtyTimer != null) window.clearTimeout(checkDirtyTimer);
            activeForm = null; // allow nexuses to be opened again
            findNexus(data, 0);
        });
    }
}

function display_error_field(field) { field.css('border', '3px solid red'); }
function clear_error_field(field) { field.css('border', '1px solid gray'); }

function timePointInputBox(id, timePoint) {
    var val = (timePoint == null ? '' : formatTimePoint(timePoint));
    return $('<input type="text" class="editNexusField editNexusDateField" id="nedit_' + id + '" value="' + val + '" '+jsFieldDirtyChecks()+'/>');
}

function update_tag_display_name (id, name) {
    if (name.lastIndexOf('http://', 0) === 0 || name.lastIndexOf('https://', 0) === 0) {
        name = '<a target="_blank" href=' + name + '>' + window.decodeURI(name) + "</a>";
    } else if (name == "automated_entry_please_verify") {
        name = '<a style="text-decoration: underline; color: black;" onclick="return false;" href="." title="Coming Soon! Support for user-contributed updates!">' + name + "</a>";
    }
    if (activeForm != 'editNexus') {
        $('#'+id).html(newSearchLink(name));
    } else {
        $('#'+id).html(name);
    }
}

function closeNexusDetails () {
    if (activeForm != 'editNexus') {
        var container = $('#nexusDetailsContainer');
        container.css('zIndex', -1);
        enableNexusEditButtons(false);
        Histori.active_nexus = null;
    }
}

function viewNexusVersions () {
    // todo
}
