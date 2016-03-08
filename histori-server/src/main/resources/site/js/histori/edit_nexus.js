
TAG_TYPES = ['event_type', 'world_actor', 'result', 'impact', 'person', 'event', 'citation', 'idea', 'meta'];
TAG_TYPE_NAMES = ['event types', 'world actors', 'results', 'impacts', 'persons', 'events', 'citations', 'ideas', 'meta'];

function formatEditTimePoint(date) {
    if (typeof date == "undefined" || date == null) return "";
    var year = date.year;
    var month = (typeof date.month == "undefined" || date.month == null) ? "" : "-" + date.month;
    var day = (typeof date.day == "undefined" || date.day == null) ? "" : "-" + date.day;

    return year + month + day;
}
function formatTimePoint(date) {
    if (typeof date == "undefined" || date == null) return "";
    var year = date.year;
    var eraMultiplier = (year < 0) ? -1 : 1;
    var suffix = (year < 0) ? ' BCE' : '';
    var month = (typeof date.month == "undefined" || date.month == null) ? "" : MONTHS[date.month];
    var day = (typeof date.day == "undefined" || date.day == null) ? "" : date.day;

    return day + ' ' + month + ' ' + (eraMultiplier * year) + suffix;
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

function openNexusDetails (uuid, tries) {
    closeForm();
    var nexusSummary = nexusSummariesByUuid[uuid];

    if (typeof nexusSummary == "undefined" || nexusSummary == null) return;
    if (typeof nexusSummary.dirty != "undefined" && nexusSummary.dirty) {
        $('#btn_nexusSave').css('visibility', 'visible');
    } else {
        $('#btn_nexusSave').css('visibility', 'hidden');
    }

    activeNexusSummary = nexusSummary;
    var nexus = activeNexusSummary.primary;

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
        commentaryContainer.append('<p class="commentaryMarkdown">'+markupConverter.makeHtml(nexus.markdown)+'</p>');
    }

    var tagsContainer = $('#nexusTagsContainer');
    tagsContainer.empty();

    if (nexusSummary.incomplete) {
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
                    listOfTags += "<div class='nexusTag'><div class='nexusTag_tagName' id='" + nexusTagId + "'>" + newSearchLink(tags[j].displayName) + "</div>";
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
    var container = $('#nexusDetailsContainer');
    container.css('zIndex', 1);
}

function findNexus(nexus, tries) {
    Api.find_nexus(nexus.uuid, function (data) {
        console.log('find_nexus for try #' + tries + ' returned nexus, incomplete=' + data.incomplete);
        nexusSummariesByUuid[nexus.uuid] = data;
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

function update_time_point (timePoint, value) {

    if (typeof value == "undefined" || value == null) return false;

    var dateParts = value.split('-');
    if (dateParts.length == 0 || dateParts.length > 6 || (dateParts.length == 1 && dateParts[0].length == 0)) return false;

    timePoint.year = parseInt(dateParts[0]);
    var instant = '' + timePoint.year;
    if (dateParts.length > 1) {
        if (dateParts[1] < 1 || dateParts[1] > 12) return false;
        timePoint.month = parseInt(dateParts[1]);
        instant += (timePoint.month < 10 ? "0" : "") + timePoint.month;
    } else {
        instant += '00';
    }
    if (dateParts.length > 2) {
        if (dateParts[2] < 1 || dateParts[2] > 31) return false;
        timePoint.day = parseInt(dateParts[2]);
        instant += (timePoint.day < 10 ? "0" : "") + timePoint.day;
    } else {
        instant += '00';
    }
    if (dateParts.length > 3) {
        if (dateParts[3] < 0 || dateParts[3] > 23) return false;
        timePoint.hour = parseInt(dateParts[3]);
        instant += (timePoint.hour < 10 ? "0" : "") + timePoint.hour;
    } else {
        instant += '00';
    }
    if (dateParts.length > 4) {
        if (dateParts[4] < 1 || dateParts[4] > 59) return false;
        timePoint.minute = parseInt(dateParts[4]);
        instant += (timePoint.minute < 10 ? "0" : "") + timePoint.minute;
    } else {
        instant += '00';
    }
    if (dateParts.length > 5) {
        if (dateParts[5] < 1 || dateParts[5] > 59) return false;
        timePoint.second = parseInt(dateParts[5]);
        instant += (timePoint.second < 10 ? "0" : "") + timePoint.second;
    } else {
        instant += '00';
    }
    timePoint.instant = parseInt(instant);
    return true;
}

function commitNexusEdits () {
    //save_fields();

    if (activeNexusSummary != null) {
        var nexusSummary = nexusSummariesByUuid[activeNexusSummary.uuid];
        if (typeof nexusSummary != "undefined" && typeof nexusSummary.primary != "undefined") {
            Histori.edit_nexus(nexusSummary.primary, function (data) {
                findNexus(data, 0);
            });
            nexusSummary.dirty = false;
        }
    }
}

function save_field(name) {
    console.log('save_field: '+name);
    if (name == "nexusRangeStart" || name == "nexusRangeEnd") {
        var timeRange = { startPoint: {}, endPoint: {} };
        var startField = $('.edit_nexusRangeStart');
        var endField = $('.edit_nexusRangeEnd');
        var ok = true;
        if (!update_time_point(timeRange.startPoint, startField.val())) {
            display_error_field(startField);
            ok = false;
        }
        var endVal = endField.val();
        if (typeof endVal == "undefined" || endVal.length == 0) {
            timeRange.endPoint = null;
        } else {
            if (!update_time_point(timeRange.endPoint, endVal)) {
                display_error_field(endField);
                ok = false;
            }
        }
        if (timeRange.endPoint != null && timeRange.endPoint.instant < timeRange.startPoint.instant) {
            display_error_field(endField);
            ok = false;
        }
        if (!ok) return false;

        // todo: if it did not change, do not mark as dirty
        nexusSummariesByUuid[activeNexusSummary.uuid].dirty = true;
        console.log('updating in-mem timeRange: '+timeRange);
        nexusSummariesByUuid[activeNexusSummary.uuid].primary.timeRange = timeRange;

        $('#btn_nexusSave').css('visibility', 'visible');

        var rangeStart = $('.nexusRangeStart');
        var rangeEnd = $('.nexusRangeEnd');
        rangeStart.off("keyup");
        rangeEnd.off("keyup");
        clear_error_field(startField);
        clear_error_field(endField);
        displayTimeRange(timeRange);
    }
}

function display_error_field(field) { field.css('border', '3px solid red'); }
function clear_error_field(field) { field.css('border', 'none'); }

function timePointInputBox(id, timePoint) {
    var val = (timePoint == null ? '' : formatEditTimePoint(timePoint));
    return $('<input type="text" class="edit_' + id + '" value="' + val + '"/>');
}
function edit_nexus_field (id) {
    if (id == "nexusRangeStart" || id == "nexusRangeEnd") {
        console.log('edit_nexus_field (range): '+id);
        var timeRange = activeNexusSummary.primary.timeRange;
        var startField = $('.edit_nexusRangeStart');
        var endField = $('.edit_nexusRangeEnd');

        if (!startField.length) startField = timePointInputBox('nexusRangeStart', timeRange.startPoint);
        if (!endField.length) endField = timePointInputBox('nexusRangeEnd', timeRange.endPoint);

        var rangeStart = $('.nexusRangeStart');
        rangeStart.on("keyup", function (e) {
            if (e.keyCode == 13) save_field('nexusRangeStart');
        });
        var rangeHyphen = $('.nexusRangeHyphen');
        var rangeEnd = $('.nexusRangeEnd');
        rangeEnd.on("keyup", function (e) {
            if (e.keyCode == 13) save_field('nexusRangeEnd');
        });
        rangeStart.empty(); rangeHyphen.empty(); rangeEnd.empty();
        rangeStart.append(startField);
        rangeHyphen.html(' - ');
        rangeEnd.append(endField);
        if (id == "nexusRangeStart") startField.focus();
        if (id == "nexusRangeEnd") endField.focus();
    } else {
        console.log('edit_nexus_field (unknown): ' + id);
    }
}

function update_tag_display_name (id, displayName) {
    if (displayName.lastIndexOf('http://', 0) === 0 || displayName.lastIndexOf('https://', 0) === 0) {
        displayName = '<a target="_blank" href=' + displayName + '>' + window.decodeURI(displayName) + "</a>";
    } else if (displayName == "automated_entry_please_verify") {
        displayName = '<a style="text-decoration: underline; color: black;" onclick="return false;" href="." title="Coming Soon! Support for user-contributed updates!">' + displayName + "</a>";
    }
    $('#'+id).html(newSearchLink(displayName));
}

function closeNexusDetails () {
    var container = $('#nexusDetailsContainer');
    container.css('zIndex', -1);
    $('#btn_nexusSave').css('visibility', 'hidden');
    activeNexusSummary = null;
}

function viewNexusVersions () {
    // todo
}
