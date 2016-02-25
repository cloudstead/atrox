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

// Must match what is in histori-config.xml, then add a trailing slash here
API_PREFIX = "/api/";

Api = {
    // must match API_TOKEN in ApiConstants.java
    API_TOKEN: 'x-histori-api-key',

    _get: function (url, success, fail) {
        if (get_token() == NO_TOKEN) Api.create_anonymous_session();
        var results = null;
        $.ajax({
            type: 'GET',
            url: API_PREFIX + url,
            async: (typeof success != "undefined" && success != null),
            beforeSend: add_api_auth,
            success: function (data, status, jqXHR) {
                results = data;
                if (typeof success != "undefined" && success != null) success(data);
            },
            error: function (jqXHR, status, error) {
                if (typeof fail != "undefined" && fail != null) fail(jqXHR, status, error);
            }
        });
        return results;
    },

    _update: function (method, url, data, success, fail, skipTokenCheck) {
        if (get_token() == NO_TOKEN && (typeof skipTokenCheck == "undefined") && skipTokenCheck == false) Api.create_anonymous_session();
        var result = null;
        $.ajax({
            type: method,
            url: API_PREFIX + url,
            async: (typeof success != "undefined" && success != null),
            contentType: 'application/json',
            data: JSON.stringify(data),
            beforeSend: add_api_auth,
            success: function (response, status, jqXHR) {
                result = response;
                if (typeof success != "undefined" && success != null) success(data);
            },
            error: function (jqXHR, status, error) {
                alert('error POSTing to '+url+': '+error);
                console.log('setup error: status='+status+', error='+error);
                if (typeof fail != "undefined" && fail != null) fail(jqXHR, status, error);
            }
        });
        return result;
    },

    _post: function(url, data, success, fail, skipTokenCheck) { return Api._update('POST', url, data, success, fail, skipTokenCheck); },
    _put:  function(url, data, success, fail) { return Api._update('PUT', url, data, success, fail); },

    _delete: function (path, success, fail) {
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
                alert('error deleting '+path+': '+error);
                if (typeof fail != "undefined" && fail != null) fail(jqXHR, status, error);
            }
        });
        return ok;
    },

    login: function (email, password, success, fail) { return Api._post('accounts/login', {'name': email, 'password': password}, success, fail); },

    register: function (email, password, success, fail) { return Api._post('accounts/register', {'name': email, 'password': password}, success, fail); },

    create_anonymous_session: function () {
        var auth_response = Api._post('accounts/register', {}, null, null, true);
        if (typeof auth_response != "undefined" && auth_response != null) {
            sessionStorage.setItem('histori_session', auth_response.sessionId);
            Histori.set_account(auth_response.account);
        }
    },

    _find_nexuses_timer: -1,
    _find_nexuses_request: [],

    find_nexuses: function (origin, current, north, south, east, west, success, fail) {
        //console.log('find_nexuses::'+origin+", "+current);
        Api._find_nexuses_request = [origin, current, north, south, east, west];
        if (Api._find_nexuses_timer != -1) {
            window.clearTimeout(Api._find_nexuses_timer);
        }
        Api._find_nexuses_timer = window.setTimeout(function () {
            var start = Api._find_nexuses_request[0];
            var end = Api._find_nexuses_request[1];
            var north = Api._find_nexuses_request[2];
            var south = Api._find_nexuses_request[3];
            var east = Api._find_nexuses_request[4];
            var west = Api._find_nexuses_request[5];
            //console.log('NOW calling API .... find_nexuses:'+ start+", "+ end);
            Api._get('search/date/'+start+'/'+end+'/'+north+'/'+south+'/'+east+'/'+west, success, fail);
        }, 1000);
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

    transform_image: function (src, width, height) {
        return src.replace("/public/image/", "/public/transform/w_"+width+"-h_"+height+"/");
    }

    //addRegion: function (email, password) { return Api._post('/api/accounts/' + email, {'name': email, 'password': password}); }

};