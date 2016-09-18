
TAG_TYPES = ['event_type', 'world_actor', 'result', 'impact', 'person', 'event', 'citation', 'idea', 'meta'];
TAG_TYPE_NAMES = ['event types', 'world actors', 'results', 'impacts', 'persons', 'events', 'citations', 'ideas', 'meta'];

function formatTimePoint(date) {
    if (typeof date == "undefined" || date == null) return "";
    var year = date.year;
    var eraMultiplier = (year < 0) ? -1 : 1;
    var suffix = (year < 0) ? ' BCE' : '';
    var month = (typeof date.month == "undefined" || date.month == null) ? "" : MONTHS[date.month];
    var day = (typeof date.day == "undefined" || date.day == null) ? "" : date.day;

    return (day + ' ' + month + ' ' + (eraMultiplier * year) + suffix).trim();
}

var activeNexusSummary = null;
var markupConverter = new showdown.Converter()

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
function openNexusDetails (uuid, tries) {

    closeForm();
    var container = $('#nexusDetailsContainer');
    var nexus = Api.nexusCache[uuid];
    Histori.active_nexus = nexus;

    if (typeof nexus == "undefined" || nexus == null) return;
    enableNexusEditButtons(false);

    $('#nexusNameContainer').html(nexus.name);
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

    if (nexus.incomplete) {
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
                    var nexusTagId = 'nexusTag_' + tags[j].uuid + '_' + tags[j].tagName;
                    listOfTags += "<div class='nexusTag'><div class='nexusTag_tagName' id='" + nexusTagId + "'>" + newSearchLink(tags[j].tagName) + "</div>";
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
    if (typeof timeRange.endPoint != "undefined" && timeRange.endPoint != null && ((typeof timeRange.endPoint.year != "undefined") && timeRange.endPoint.year != null)) {
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
    return false;
}

function jsFieldDirtyChecks () { return 'onkeyup="checkDirty()" onchange="checkDirty()" onblur="checkDirty()"'; }

function startEditingNexus () {
    if (isAnonymous()) {
        showLoginForm();
        return;
    }
    closeForm();
    var container = $('#nexusDetailsContainer');
    var nexus = Histori.active_nexus;
    if (typeof nexus == "undefined" || nexus == null) return;
    enableNexusEditButtons(true);

    // create name fields
    var nameContainer = $('#nexusNameContainer');
    nameContainer.empty();
    nameContainer.append($('<span class="editNexusLabel">Name:</span>'));
    nameContainer.append($('<input class="editNexusField" id="nedit_name" type="text" name="name" value="'+nexus.name+'" '+jsFieldDirtyChecks()+'>'));

    // set author label and add visibility field
    var authorContainer = $('#nexusAuthorContainer');
    authorContainer.append($('<span>edited by: '+Histori.account().name+'</span>'));
    authorContainer.append($('<br/>'));
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
    rangeStart.append($('<span class="editNexusLabel">Date(s):</span>'));
    rangeStart.append(startField);
    rangeHyphen.html(' to ');
    rangeEnd.append(endField);

    // create geo fields and map-edit button
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

    if (nexus.incomplete) {
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
                    var nexusTagId = 'nexusTag_' + tags[j].uuid + '_' + tags[j].tagName;
                    listOfTags += "<div class='nexusTag'><div class='nexusTag_tagName' id='" + nexusTagId + "'>" + newSearchLink(tags[j].tagName) + "</div>";
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

    if (nexus.bounds.east > map.getBounds().getCenter().lng()) {
        container.css('left', '20px');
        container.css('right', '');
    } else {
        container.css('left', '');
        container.css('right', '20px');
    }

    container.css('zIndex', 1);
}

function cancelNexusEdits () {
    if (checkDirtyTimer != null) window.clearTimeout(checkDirtyTimer);
    if (Histori.active_nexus) {
        Histori.active_nexus.dirty = false;
        openNexusDetails(Histori.active_nexus.uuid, 0);
    }
}

function populateEditedNexus () {
    var edits = {};
    edits.name = $('#nedit_name').val();
    edits.visibility = $('#nedit_visibility option:selected').val();
    edits.timeRange = {
        startPoint: { inputString: $('#nedit_nexusRangeStart').val() },
        endPoint: { inputString: $('#nedit_nexusRangeEnd').val() }
    };
    edits.point = { type: 'Point', coordinates: [ 0.0, 0.0 ] };
    edits.markdown = $('#nedit_markdown').val();
    return edits;
}

function commitNexusEdits () {
    if (Histori.active_nexus != null && isDirty(Histori.active_nexus)) {
        Api.edit_nexus(Histori.active_nexus, populateEditedNexus(), function (data) {
            Histori.active_nexus.dirty = false;
            Histori.active_nexus = data;
            if (checkDirtyTimer != null) window.clearTimeout(checkDirtyTimer);
            newExactSearch();
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
    $('#'+id).html(newSearchLink(name));
}

function closeNexusDetails () {
    var container = $('#nexusDetailsContainer');
    container.css('zIndex', -1);
    enableNexusEditButtons(false);
    Histori.active_nexus = null;
}

function viewNexusVersions () {
    // todo
}
