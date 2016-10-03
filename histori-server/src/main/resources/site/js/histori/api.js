// from https://stackoverflow.com/a/105074/1251543
function guid() {
    function s4() {
        return Math.floor((1 + Math.random()) * 0x10000)
            .toString(16)
            .substring(1);
    }
    return s4() + s4() + '-' + s4() + '-' + s4() + '-' +
        s4() + '-' + s4() + s4() + s4();
}

NO_TOKEN = "-no-token-";
function get_token() {
    return sessionStorage.getItem('histori_session') || NO_TOKEN;
}

function add_api_auth (xhr) {
    var token = get_token();
    xhr.setRequestHeader(Api.API_TOKEN, token);
}

// Must match what is in histori-config.yml, then add a trailing slash here
API_PREFIX = "/api/";

Api = {
    // must match API_TOKEN in ApiConstants.java
    API_TOKEN: 'x-histori-api-key',

    _get: function (url, success, fail) {
        if (get_token() == NO_TOKEN) Api.create_anonymous_session();
        var result = null;
        $.ajax({
            type: 'GET',
            url: API_PREFIX + url,
            async: (typeof success != "undefined" && success != null),
            beforeSend: add_api_auth,
            success: function (data, status, jqXHR) {
                result = data;
                if (typeof success != "undefined" && success != null) success(result);
            },
            error: function (jqXHR, status, error) {
                if (jqXHR.status == 200 && typeof success != "undefined" && success != null) {
                    success(jqXHR.responseText);
                } else if (typeof fail != "undefined" && fail != null) {
                    console.log('_get error: status='+status+' (jqXHR.status='+jqXHR.status+'), error='+error);
                    fail(jqXHR, status, error);
                }
            }
        });
        return result;
    },

    _update: function (method, url, data, success, fail, skipTokenCheck) {
        if (get_token() == NO_TOKEN && (typeof skipTokenCheck == "undefined" || skipTokenCheck == false)) Api.create_anonymous_session();
        var result = null;
        $.ajax({
            type: method,
            url: API_PREFIX + url,
            async: (typeof success != "undefined" && success != null),
            contentType: 'application/json',
            data: JSON.stringify(data),
            beforeSend: add_api_auth,
            success: function (data, status, jqXHR) {
                result = data;
                if (typeof success != "undefined" && success != null) success(result);
            },
            error: function (jqXHR, status, error) {
                console.log('_update error: status='+status+' (jqXHR.status='+jqXHR.status+'), error='+error);
                if (typeof fail != "undefined" && fail != null) fail(jqXHR, status, error);
            }
        });
        return result;
    },

    _post: function(url, data, success, fail, skipTokenCheck) { return Api._update('POST', url, data, success, fail, skipTokenCheck); },
    _put:  function(url, data, success, fail) { return Api._update('PUT', url, data, success, fail); },

    _delete: function (url, success, fail) {
        var ok = false;
        $.ajax({
            type: 'DELETE',
            url: API_PREFIX + url,
            async: (typeof success != "undefined" && success != null),
            beforeSend: add_api_auth,
            'success': function (accounts, status, jqXHR) {
                ok = true;
                if (typeof success != "undefined" && success != null) success();
            },
            'error': function (jqXHR, status, error) {
                if (jqXHR.status == 200 && typeof success != "undefined" && success != null) {
                    success();
                } else if (typeof fail != "undefined" && fail != null) {
                    console.log('_delete error: status='+status+' (jqXHR.status='+jqXHR.status+'), error='+error);
                    fail(jqXHR, status, error);
                }
            }
        });
        return ok;
    },

    login: function (email, password, success, fail) {
        return Api._post('accounts/login', {'name': email, 'password': password}, success, fail);
    },

    register: function (reg, success, fail) {
        return Api._post('accounts/register', reg, success, fail);
    },

    forgot_password: function (email, success, fail) {
        Api._post('accounts/forgot_password', email, success, fail);
    },

    reset_password: function (key, password, success, fail) {
        Api._post('accounts/reset_password', {'token': key, 'password': password}, success, fail);
    },

    update_account: function (info, success, fail) {
        Api._post('accounts', info, success, fail);
    },

    remove_account: function (info, success, fail) {
        Api._post('accounts/remove', info, success, fail);
    },

    create_anonymous_session: function () {
        var auth_response = Api._post('accounts/register', {}, null, null, true);
        if (typeof auth_response != "undefined" && auth_response != null) {
            sessionStorage.setItem('histori_session', auth_response.sessionId);
            Histori.set_account(auth_response.account);
        }
    },

    get_config: function (name, success, fail) {
        return Api._get('configs/'+name, success, fail);
    },

    _find_nexuses_timers: {},

    // Hash of nexus UUID -> nexus
    nexusCache: {},

    find_nexuses: function (search_id, search, start_func, success, fail) {
        //console.log('find_nexuses::'+origin+", "+current);
        if (typeof Api._find_nexuses_timers[search_id] != "undefined") {
            window.clearTimeout(Api._find_nexuses_timers[search_id]);
        }
        var timeout = getParameterByName('timeout');
        if (typeof timeout == "undefined" || timeout == null || timeout.length == 0) timeout = null;

        var useCache = to_bool(getParameterByName('use-cache'));
        var authoritativeParam = getParameterByName('authoritative');
        var authoritative = !authoritativeParam || authoritativeParam.trim().length == 0 || to_bool(authoritativeParam);

        var s = search;
        Api._find_nexuses_timers[search_id] = window.setTimeout(function () {
            start_func();
            var search_uri = 'search/q/'+s.start+'/'+s.end+'/'+s.north+'/'+s.south+'/'+s.east+'/'+s.west+'?q='+encodeURIComponent(s.query)
                + (timeout != null ? '&t='+timeout : '')
                + (useCache != null ? '&c='+useCache : '')
                + (!authoritative ? '&a=false' : '');
            console.log('searching('+search_id+'): '+search_uri);
            Api._get(search_uri, function (data) {
                // adjust data model: set version if absent, move tags up one level
                // also, set in cache. a nexus is immutable.
                if (data && data.results && data.results instanceof Array) {
                    for (var i = 0; i < data.results.length; i++) {
                        var result = data.results[i];
                        if (typeof result.primary != "undefined" && result.primary != null) {
                            var nexus = result.primary;
                            if (typeof nexus.version == "undefined" || nexus.version == null) nexus.version = 0;
                            if (nexus.tags) nexus.tags = nexus.tags.tags;
                            Api.nexusCache[nexus.uuid] = nexus;
                        }
                    }
                }
                success(data);
            }, fail);
        }, 2000);
    },

    find_nexus_exact: function (uuid, success, fail) {
        Api._get('search/n/'+uuid+"?exact=true", function (nexus) {
            if (typeof nexus.version == "undefined" || nexus.version == null) nexus.version = 0;
            if (nexus.tags) nexus.tags = nexus.tags.tags;
            Api.nexusCache[uuid] = nexus.primary;
            success(nexus);
        }, fail);
    },

    autocomplete: function (matchType) {
        var uri = '/api/autocomplete';
        var hasMatch = (typeof matchType != "undefined" && matchType != null);
        if (hasMatch) return uri + '/' + includes.join("_");
        return uri;
    },

    upload_image: function (xhr, file, success, fail) {

        if (get_token() == NO_TOKEN || get_token() == "undefined") Api.create_anonymous_session();

        xhr.open('post', API_PREFIX + 'map_images', true);

        var fd = new FormData;
        fd.append('file', file);

        add_api_auth(xhr);
        xhr.onreadystatechange = function() {
            if (xhr.readyState == 4) {
                if (xhr.status == 200) {
                    if (typeof success != "undefined" && success != null) success(xhr);
                } else {
                    if (typeof fail != "undefined" && fail != null) fail(xhr);
                }
            }
        };
        xhr.send(fd);
    },

    _tag_cache: null, // todo: fix tag caching

    /**
     * resolve tags
     * @param tagsToIds an array of {tag: 'tag-name', id: 'dom-id'}
     * @param callback the callback to pass the tag name and DOM id to
     */
    resolve_tags: function (tagsToIds, callback) {
        var tagMap = {};
        var tags = [];
        if (tagsToIds.length == 0) return;
        var added = 0;
        for (var i=0; i<tagsToIds.length; i++) {
            var spec = tagsToIds[i];
            if (Api._tag_cache != null && typeof Api._tag_cache[spec.tag] != "undefined") {
                callback(spec.id, Api._tag_cache[spec.tag]);
            } else {
                if (typeof tagMap[spec.tag] == "undefined") {
                    tagMap[spec.tag] = [];
                    tags.push(spec.tag);
                }
                tagMap[spec.tag].push(spec.id);
                if (added++ > 100) {
                    var remainder = tagsToIds.slice(i);
                    window.setTimeout(Api._resolve_remainder(remainder, callback), 100);
                    break;
                }
            }
        }
        if (tags.length == 0) return; // we found everything in cache. cool.

        Api._post('tags/resolve', tags, function (data, status, jqXHR) {
            if (typeof data == "undefined" || !is_array(data)) return;
            for (var i=0; i<data.length; i++) {
                var tag = data[i];
                if (typeof tagMap[tag.canonicalName] != "undefined" && is_array(tagMap[tag.canonicalName])) {
                    var ids = tagMap[tag.canonicalName];
                    for (var j=0; j<ids.length; j++) {
                        if (Api._tag_cache != null) { Api._tag_cache[spec.tag] = data[i].name; }
                        callback(ids[j], data[i].name);
                    }
                }
            }
        }, null, true);
    },

    _resolve_remainder: function (tagsToIds, callback) {
        return function () { Api.resolve_tags(tagsToIds, callback); }
    },

    owner_name: function (uuid, id, prefix) {
        Api._get('tags/owner/'+uuid, function (data) {
            $(id).html(prefix + " " + data);
        });
    },

    edit_nexus: function (nexus, edited, success, fail) {
        if (edited.name != nexus.name) {
            Api._put('nexus/' + encodeURIComponent(edited.name), edited, success, fail);
        } else {
            Api._post('nexus/' + encodeURIComponent(nexus.name) + '/' + nexus.uuid, edited, success, fail);
        }
    },

    // Bookmark endpoints
    get_bookmarks: function (success, fail) {
        Api._get('bookmarks', success, fail);
    },
    get_bookmark: function (name, success, fail) {
        Api._get('bookmarks/'+encodeURIComponent(name), success, fail);
    },
    add_bookmark: function (name, state, success, fail) {
        Api._put('bookmarks/'+encodeURIComponent(name), state, success, fail);
    },
    update_bookmark: function (name, state, success, fail) {
        Api._post('bookmarks/'+encodeURIComponent(name), state, success, fail);
    },
    remove_bookmark: function (name, success, fail) {
        Api._delete('bookmarks/'+encodeURIComponent(name), success, fail);
    },
    make_permalink: function (name, success, fail) {
        Api._get('bookmarks/'+encodeURIComponent(name)+'/permalink', success, fail);
    },
    get_permalink: function (link_id, success, fail) {
        Api._get('permalinks/'+encodeURIComponent(link_id), success, fail);
    },
    get_standard_permalinks: function (success, fail) {
        Api._get('permalinks', success, fail);
    },

    transform_image: function (src, width, height) {
        return src.replace("/public/image/", "/public/transform/w_"+width+"-h_"+height+"/");
    },

    _date_cache: {},

    parse_date: function (dateString) {
        if (!Api._date_cache[dateString]) {
            Api._date_cache[dateString] = Api._post('utils/date/parse', dateString);
        }
        return Api._date_cache[dateString];
    },

    find_preferred_authors: function (success, fail) { return Api._get('prefers', success, fail); },
    find_blocked_authors: function (success, fail) { return Api._get('blocks', success, fail); },

    add_preferred_author: function (author, success, fail) {
        if (!author.priority || isNaN(author.priority)) author.priority = 0;
        return Api._put('prefers', author, success, fail);
    },
    add_blocked_author: function (author, success, fail) {
        return Api._put('blocks', author, success, fail);
    },

    remove_preferred_author: function (uuid, success, fail) { return Api._delete('prefers/'+uuid, success, fail); },
    remove_blocked_author: function (uuid, success, fail) { return Api._delete('blocks/'+uuid, success, fail); },

    toggle_preferred_author: function (uuid, success, fail) { return Api._post('prefers/'+uuid+'/toggleActive', {}, success, fail); },
    toggle_blocked_author: function (uuid, success, fail) { return Api._post('blocks/'+uuid+'/toggleActive', {}, success, fail); },

    find_my_nexuses: function (success, fail) { return Api._get('accounts/n', success, fail); },
    remove_nexus: function (uuid, success, fail) { return Api._delete('accounts/n/'+uuid, success, fail); }

};