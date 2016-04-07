
// From: http://stackoverflow.com/a/210733
jQuery.fn.center = function () {
    this.css("position","absolute");
    this.css("top", Math.max(0, (($(window).height() - $(this).outerHeight()) / 2) +
            $(window).scrollTop()) + "px");
    this.css("left", Math.max(0, (($(window).width() - $(this).outerWidth()) / 2) +
            $(window).scrollLeft()) + "px");
    return this;
};

jQuery.fn.centerTop = function (offset) {
    if (typeof offset == "undefined") offset = 20;
    keep_centerTop(offset)(this);
    this.resize(keep_centerTop(offset));
    return this;
};

function keep_centerTop (offset) {
    return function (jqElement) {
        jqElement.css("position", "absolute");
        jqElement.css("top", offset + "px");
        jqElement.css("left", Math.max(0, (($(window).width() - $(jqElement).outerWidth()) / 2) + $(window).scrollLeft()) + "px");
    };
}

jQuery.fn.centerBottom = function (offset) {
    if (typeof offset == "undefined") offset = 20;
    keep_centerBottom(offset)(this);
    this.resize(keep_centerBottom(offset));
    return this;
};

function keep_centerBottom (offset) {
    return function (jqElement) {
        jqElement.css("position", "absolute");
        jqElement.css("top", ($(window).height() - offset) + "px");
        jqElement.css("left", Math.max(0, (($(window).width() - $(jqElement).outerWidth()) / 2) + $(window).scrollLeft()) + "px");
    };
}

MONTHS = [null,'January','February','March', 'April','May','June','July','August','September','October','November','December'];
MONTH_SHORT_NAMES = [null, 'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

function parseMonth(val) {
    var i;
    for (i=1; i<MONTH_SHORT_NAMES.length; i++) {
        if (val == MONTH_SHORT_NAMES[i]) return i;
    }
    for (i=1; i<MONTHS.length; i++) {
        if (val == MONTHS[i]) return i;
    }
    return parseInt(val);
}

function is_array (x) {
    return Object.prototype.toString.call( x ) === '[object Array]'
}

function isAnonymous() {
    return (get_token() == NO_TOKEN || ((typeof Histori.account() == 'undefined') || (typeof Histori.account().email == 'undefined' || Histori.account().anonymous)));
}

var activeForm = null;
function showForm(id, position_func, position_arg) {
    var container = $('#'+id);
    if (container.css('z-index') > 0) {
        container.css('z-index', -1);
    } else {
        if (activeForm != null) {
            closeForm(activeForm);
        }
        if (typeof position_func != "undefined") {
            if (typeof position_arg != "undefined") {
                position_func.call(container, position_arg);
            } else {
                position_func.call(container);
            }
        } else {
            container.center();
        }
        container.css({zIndex: 1, visibility: 'visible'});
        activeForm = id;
    }
}

function closeForm(id) {
    if (typeof id == "undefined" || id == null) {
        id = activeForm;
    }
    var container = $('#' + id);
    container.css('z-index', -1);
    $(".authError").empty();
}

String.prototype.trim = String.prototype.trim || function trim() { return this.replace(/^\s\s*/, '').replace(/\s\s*$/, ''); };

String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};

// adapted from https://stackoverflow.com/a/13538245/1251543
String.prototype.escape = function() {
    var tagsToReplace = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '\'': '&apos;'
    };
    return this.replace(/[&<>]/g, function(tag) {
        return tagsToReplace[tag] || tag;
    });
};

String.prototype.replaceAll = function(search, replacement) {
    var target = this;
    return target.replace(new RegExp(search, 'g'), replacement);
};

// From: http://werxltd.com/wp/2010/05/13/javascript-implementation-of-javas-string-hashcode-method/
String.prototype.hashCode = function() {
    var hash = 0, i, chr, len;
    if (this.length === 0) return hash;
    for (i = 0, len = this.length; i < len; i++) {
        chr   = this.charCodeAt(i);
        hash  = ((hash << 5) - hash) + chr;
        hash |= 0; // Convert to 32bit integer
    }
    return hash;
};

// From: https://stackoverflow.com/a/901144/1251543
function getParameterByName(name, url) {
    if (!url) url = window.location.href;
    name = name.replace(/[\[\]]/g, "\\$&");
    var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

function display_bounds(bounds) {
    if ((parseFloat(bounds.north) - parseFloat(bounds.south) < 0.001)
        && (parseFloat(bounds.east) - parseFloat(bounds.west) < 0.001)) {
        var northOrSouth = (bounds.north > 0) ? bounds.north.toFixed(3)+'&#176;N' : (-1.0*bounds.south.toFixed(3))+'&#176;S';
        var eastOrWest = (bounds.east > 0) ? bounds.east.toFixed(3)+'&#176;E' : (-1.0*bounds.west.toFixed(3))+'&#176;W';
        return 'at ' + northOrSouth + ', ' + eastOrWest;
    }
    return bounds.south.toFixed(3)+'&#176;S, '+bounds.west.toFixed(3)+'&#176;W to<br/>'
        + bounds.north.toFixed(3)+'&#176;N, '+bounds.east.toFixed(3)+'&#176;E';
}

function display_range(range) {
    var start = slider.label_for_raw(parseFloat(range.start));
    var end = slider.label_for_raw(parseFloat(range.end));

    if (start.endsWith(' CE') && end.endsWith(' CE')) {
        start = start.substring(0, start.length - ' CE'.length)
    } else if (start.endsWith(' BCE') && end.endsWith(' BCE')) {
        start = start.substring(0, start.length - ' BCE'.length)
    }

    return start + ' to ' + end;
}

function toggle_visibility (elementId, html) {
    var element = $('#'+elementId);
    if ( element.css('visibility') == 'visible' ) {
        element.css('visibility', 'hidden');
        element.empty();
    } else {
        element.css('visibility', 'visible');
        if (typeof html != "undefined") {
            element.html(html);
        }
    }
}

DEFAULT_STATE = {
    timeline: {
        range: {start: 1500, end: 2016},
        zoom_stack: [{start: -10000, end: 2016}, {start: -4000, end: 2016}]
    },
    map:{north:48.8544605754133,south:15.523358948698112,east:153.193359375,west:27.0703125},
    //map: {
    //    north: 60,
    //    south: 60,
    //    east: 90,
    //    west: 90
    //},
    searches: []
};

Histori = {

    json_safe_parse: function (j) {
        return j ? JSON.parse(j) : null;
    },

    login: function (email, password, success, fail) {
        Api.login(email, password,
            function (auth_response) {
                success(auth_response);
                sessionStorage.setItem('histori_session', auth_response.sessionId);
                Histori.set_account(auth_response.account);
            }, fail);
    },

    register: function (fields, success, fail) {
        Api.register({
                name: fields['name'].value,
                email: fields['email'].value,
                password: fields['password'].value,
                subscribe: fields['subscribe'].checked,
                captcha: sessionStorage.getItem('g-recaptcha-response')
            },
            function (auth_response) {
                success(auth_response);
                sessionStorage.setItem('histori_session', auth_response.sessionId);
                Histori.set_account(auth_response.account);
            }, fail);
    },

    logout: function () {
        $('#btnAccount').attr('title', 'sign in / sign up');
        sessionStorage.clear();
        this.clear_session_data();
        window.location.href = location.protocol + '//' + location.host + '/';
    },

    forgot_password: function (email, success, fail) {
        Api.forgot_password(email, success, fail);
    },

    reset_password: function (key, password, success, fail) {
        Api.reset_password(key, password, success, fail);
    },

    update_account: function (fields, success, fail) {
        Api.update_account({
            name: fields['name'].value,
            email: fields['email'].value,
            currentPassword: fields['currentPassword'].value,
            newPassword: fields['newPassword'].value,
            subscribe: fields['subscribe'].checked
        }, success, fail);
    },

    remove_account: function (fields, success, fail) {
        Api.remove_account({
            name: fields['name'].value,
            email: fields['email'].value,
            password: fields['password'].value,
            captcha: sessionStorage.getItem('g-recaptcha-response')
        }, success, fail);
    },

    account: function () {
        return Histori.json_safe_parse(sessionStorage.getItem('histori_account'));
    },

    set_account: function (account) {
        sessionStorage.setItem('histori_account', JSON.stringify(account));
        if (!isAnonymous()) $('#btnAccount').attr('title', 'signed in as '+account.name+'|account info / sign out');
    },

    get_session: function (name, default_value) {
        if (sessionStorage.getItem(name)) return sessionStorage.getItem(name);
        return default_value;
    },
    set_session: function (name, value) { sessionStorage.setItem(name, value); },

    edit_nexus: function (nexus, success, fail) {
        Api.edit_nexus(nexus, success, fail);
    },

    // Session save-state functions (stored in localStorage)
    _search_state: '__histori.search_state',

    get_session_state: function () {
        var bounds = map.getBounds();
        var state = {
            timeline: {
                range: slider.range,
                zoom_stack: slider.zoom_stack
            },
            map: {
                north: bounds.getNorthEast().lat(),
                south: bounds.getSouthWest().lat(),
                east: bounds.getNorthEast().lng(),
                west: bounds.getSouthWest().lng()
            },
            searches: []
        };
        var searchRows = $('.searchRow');
        for (var i=0; i<searchRows.length; i++) {
            var id = searchRowIdFromOtherId(searchRows[i].id);
            var searchBox = rowSearchBox(id);
            var markerIcon = rowMarkerImageSrc(id);
            if (markerIcon == null) {
                // should never happen, but just in case
                markerIcon = 'markers/'+pickUnusedColor()+'_Marker_blank.png';
            }
            var search = {
                query: searchBox.val(),
                icon: markerIcon
            };
            state.searches.push(search);
        }
        return state;
    },

    // stores all searches + geo + time zoom stack so they can be restored upon page refresh
    save_session_state: function () {
        localStorage.setItem(this._search_state, JSON.stringify(this.get_session_state()));
    },

    clear_session_data: function () { localStorage.removeItem(this._search_state); },

    has_session_data: function () {
        var json = localStorage.getItem(this._search_state);
        return !(typeof json == 'undefined' || json == null);
    },

    restore_session: function () {

        var json = localStorage.getItem(this._search_state);
        if (typeof json == 'undefined' || json == null) {
            console.log('restore_session: no session data found');
            return;
        } else {
            console.log('restore_session: restored: '+json);
        }
        var state = JSON.parse(json);
        this.restore_state(state);
    },

    restore_permalink: function (link_id) {
        Api.get_permalink(link_id, function (data) {
            if (typeof data != 'undefined' && data != null) {
                if (typeof data.timeline != 'undefined' && data.timeline != null) {
                    Histori.restore_state(data);
                } else {
                    // todo: is this a tour? if so start the tour
                    console.log('restore_permalink: not a bookmark: '+JSON.stringify(data));
                }
            }
        });
    },

    restore_state: function (state) {
        remove_all_markers();

        // refresh slider
        slider.range = state.timeline.range;
        slider.zoom_stack = state.timeline.zoom_stack;
        slider.update_labels();

        // map wants a timeout for it to be ready?
        window.setTimeout(function() {
            map.fitBounds({
                north: state.map.north,
                south: state.map.south,
                east: state.map.east,
                west: state.map.west
            });
            map.setZoom(map.getZoom()+1);

            if (typeof state.searches != 'undefined' && state.searches != null && is_array(state.searches)) {

                // clear all searches
                var rows = $('.searchRow');
                for (var i=0; i<rows.length; i++) {
                    var id = searchRowIdFromOtherId(rows[i].id);
                    remove_markers(id);
                    slider.remove_markers(id);
                }
                rows.remove();

                for (var i=0; i<state.searches.length; i++) {
                    var row = newSearch(state.searches[i].query);
                    $(row).find('.searchBox_markerImage').attr('src', state.searches[i].icon);
                }
                if ($('.searchRow').length == 0) {
                    addSearchRow();
                }
            }
            refresh_map(); // new search

        }, 500);
    },

    // Bookmark functions
    // todo: refactor into 2 storage engines: local and API. Select storage engine based on anonymous vs signed-in account
    _bookmark_state: '__histori.bookmark_state',

    add_bookmark: function (name, success, fail) {
        name = name.trim();
        if (name.length == 0) {
            fail();
            return;
        }
        if (isAnonymous()) {
            this.get_bookmarks(function (bookmarks) {
                for (var i=0; i<bookmarks.length; i++) {
                    if (bookmarks[i].name == name) {
                        fail();
                        return;
                    }
                }
                var state = {
                    uuid: guid(),
                    name: name,
                    state: Histori.get_session_state()
                };
                bookmarks.push(state);
                Histori.save_local_bookmarks(bookmarks);
                success();
            });

        } else {
            Api.add_bookmark(name, Histori.get_session_state(), success, fail);
        }
        return true;
    },

    overwrite_bookmark: function (name, success, fail) {
        if (isAnonymous()) {
            this.get_bookmarks(function (bookmarks) {
                var state = Histori.get_session_state();
                for (var i=0; i<bookmarks.length; i++) {
                    if (bookmarks[i].name == name) {
                        bookmarks[i].state = state;
                        Histori.save_local_bookmarks(bookmarks);
                        success();
                        return;
                    }
                }
                fail();

            }, fail);
        } else {
            Api.update_bookmark(name, this.get_session_state(), success, fail);
        }
    },

    remove_bookmark: function (name, success, fail) {
        if (isAnonymous()) {
            this.get_bookmarks(function (bookmarks) {
                var new_bookmarks = [];
                var removed = null;
                for (var i=0; i<bookmarks.length; i++) {
                    if (bookmarks[i].name != name) {
                        new_bookmarks.push(bookmarks[i]);
                    } else {
                        if (removed != null) {
                            console.log('remove_bookmark: warning: multiple bookmarks matched name '+name);
                        }
                        removed = bookmarks[i];
                    }
                }
                Histori.save_local_bookmarks(new_bookmarks);
                success(removed.uuid);
            });
        } else {
            Api.get_bookmark(name, function (data) {
                Api.remove_bookmark(name, function () {
                    if (typeof success != "undefined" && success != null) {
                        success(data.uuid);
                    }
                }, fail);
            }, fail);
        }
    },

    get_bookmarks: function (success, fail) {
        if (isAnonymous()) {
            var json = localStorage.getItem(this._bookmark_state);
            if (typeof json == 'undefined' || json == null || json.length == 0) {
                success([]);
            } else {
                success(JSON.parse(json));
            }

        } else {
            Api.get_bookmarks(function (data) {
                var bookmarks = [];
                for (var i=0; i<data.length; i++) {
                    var bookmark = {};
                    bookmark.state = JSON.parse(data[i].json);
                    bookmark.name = data[i].name;
                    bookmark.uuid = data[i].uuid;
                    bookmarks.push(bookmark);
                }
                success(bookmarks)
            }, fail);
        }
    },

    get_bookmark: function (name, success, fail) {
        if (isAnonymous()) {
            this.get_bookmarks(function (bookmarks) {
                for (var i = 0; i < bookmarks.length; i++) {
                    if (bookmarks[i].name == name) {
                        success(bookmarks[i]);
                        return;
                    }
                }
                fail();
            });

        } else {
            Api.get_bookmark(name, function (data) {
                success({ uuid: data.uuid, name: data.name, state: JSON.parse(data.json) });
            }, fail);
        }
    },

    save_local_bookmarks: function (bookmarks) {
        localStorage.setItem(this._bookmark_state, JSON.stringify(bookmarks));
    },

    clear_local_bookmarks: function () { localStorage.removeItem(this._bookmark_state); },

    restore_bookmark_state: function (name) {
        this.get_bookmark(name, function (bookmark) {
            Histori.restore_state(bookmark.state);
        });
    }

};

