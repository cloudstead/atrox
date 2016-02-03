Api = {
    // must match API_TOKEN in ApiConstants.java
    API_TOKEN: 'x-atrox-api-key',

    _get: function (url) {
        var results = null;
        Ember.$.ajax({
            type: 'GET',
            url: url,
            async: false,
            beforeSend: add_api_auth,
            success: function (data, status, jqXHR) {
                results = data;
            }
        });
        return results;
    },

    _update: function (method, url, data) {
        var result = null;
        Ember.$.ajax({
            type: method,
            url: url,
            async: false,
            contentType: 'application/json',
            data: JSON.stringify(data),
            beforeSend: add_api_auth,
            success: function (response, status, jqXHR) {
                result = response;
            },
            error: function (jqXHR, status, error) {
                alert('error POSTing to '+url+': '+error);
                console.log('setup error: status='+status+', error='+error);
            }
        });
        return result;
    },

    _post: function(url, data) { return Api._update('POST', url, data); },
    _put:  function(url, data) { return Api._update('PUT', url, data); },

    _delete: function (path) {
        var ok = false;
        Ember.$.ajax({
            type: 'DELETE',
            url: path,
            async: false,
            beforeSend: add_api_auth,
            'success': function (accounts, status, jqXHR) {
                ok = true;
            },
            'error': function (jqXHR, status, error) {
                alert('error deleting '+path+': '+error);
            }
        });
        return ok;
    },

};