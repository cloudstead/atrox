
String.prototype.trim = String.prototype.trim || function trim() { return this.replace(/^\s\s*/, '').replace(/\s\s*$/, ''); };

String.prototype.endsWith = function(suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
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

// From: http://stackoverflow.com/a/210733
jQuery.fn.center = function () {
    this.css("position","absolute");
    this.css("top", Math.max(0, (($(window).height() - $(this).outerHeight()) / 2) +
            $(window).scrollTop()) + "px");
    this.css("left", Math.max(0, (($(window).width() - $(this).outerWidth()) / 2) +
            $(window).scrollLeft()) + "px");
    return this;
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

    register: function (name, email, password, success, fail) {
        Api.register(name, email, password,
            function (auth_response) {
                success(auth_response);
                sessionStorage.setItem('histori_session', auth_response.sessionId);
                Histori.set_account(auth_response.account);
            }, fail);
    },

    logout: function () {
        sessionStorage.clear();
    },

    forgot_password: function (email, success, fail) {
        Api.forgot_password(email, success, fail);
    },

    reset_password: function (key, password, success, fail) {
        Api.reset_password(key, password, success, fail);
    },

    update_account: function (name, email, password, success, fail) {
        // todo
    },

    account: function () {
        return Histori.json_safe_parse(sessionStorage.getItem('histori_account'));
    },

    set_account: function (account) {
        sessionStorage.setItem('histori_account', JSON.stringify(account));
    },

    get_session: function (name, default_value) {
        if (sessionStorage.getItem(name)) return sessionStorage.getItem(name);
        return default_value;
    },
    set_session: function (name, value) { sessionStorage.setItem(name, value); },

    edit_nexus: function (nexus, success, fail) {
        Api.edit_nexus(nexus, success, fail);
    }

};

