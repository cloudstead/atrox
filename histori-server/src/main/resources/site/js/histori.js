
String.prototype.trim = String.prototype.trim || function trim() { return this.replace(/^\s\s*/, '').replace(/\s\s*$/, ''); };

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

    register: function (email, success, fail) {
        Api.register(email,
            function (auth_response) {
                success(auth_response);
                sessionStorage.setItem('histori_session', auth_response.sessionId);
                Histori.set_account(auth_response.account);
            }, fail);
    },

    logout: function () {
        sessionStorage.clear();
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
    set_session: function (name, value) { sessionStorage.setItem(name, value); }

};

