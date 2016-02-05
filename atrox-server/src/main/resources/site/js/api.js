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
    return sessionStorage.getItem('atrox_session') || NO_TOKEN;
}

function add_api_auth (xhr) {
    var token = get_token();
    xhr.setRequestHeader(Api.API_TOKEN, token);
}

// Must match what is in atrox-config.xml, then add a trailing slash here
API_PREFIX = "/api/";

Api = {
    // must match API_TOKEN in ApiConstants.java
    API_TOKEN: 'x-atrox-api-key',

    _get: function (url, success, fail) {
        var results = null;
        $.ajax({
            type: 'GET',
            url: API_PREFIX + url,
            async: (arguments.length > 1),
            beforeSend: add_api_auth,
            success: function (data, status, jqXHR) {
                results = data;
                if (arguments.length > 1) success(data);
            },
            error: function (jqXHR, status, error) {
                if (arguments.length > 2) fail(jqXHR, status, error);
            }
        });
        return results;
    },

    _update: function (method, url, data, success, fail) {
        var result = null;
        $.ajax({
            type: method,
            url: API_PREFIX + url,
            async: (arguments.length > 1),
            contentType: 'application/json',
            data: JSON.stringify(data),
            beforeSend: add_api_auth,
            success: function (response, status, jqXHR) {
                result = response;
                if (arguments.length > 1) success(data);
            },
            error: function (jqXHR, status, error) {
                alert('error POSTing to '+url+': '+error);
                console.log('setup error: status='+status+', error='+error);
                if (arguments.length > 2) fail(jqXHR, status, error);
            }
        });
        return result;
    },

    _post: function(url, data, success, fail) { return Api._update('POST', url, data, success, fail); },
    _put:  function(url, data, success, fail) { return Api._update('PUT', url, data, success, fail); },

    _delete: function (path, success, fail) {
        var ok = false;
        $.ajax({
            type: 'DELETE',
            url: API_PREFIX + url,
            async: (arguments.length > 1),
            beforeSend: add_api_auth,
            'success': function (accounts, status, jqXHR) {
                ok = true;
                if (arguments.length > 1) success();
            },
            'error': function (jqXHR, status, error) {
                alert('error deleting '+path+': '+error);
                if (arguments.length > 2) fail(jqXHR, status, error);
            }
        });
        return ok;
    },

    login: function (email, password, success, fail) { return Api._post('accounts/auth/' +  encodeURIComponent(email), {'name': email, 'password': password}, success, fail); },

    register: function (email, success, fail) { return Api._post('accounts/auth/' + encodeURIComponent(email), {'name': email}, success, fail); }

    //addRegion: function (email, password) { return Api._post('/api/accounts/' + email, {'name': email, 'password': password}); }

};