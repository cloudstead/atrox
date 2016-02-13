
String.prototype.trim = String.prototype.trim || function trim() { return this.replace(/^\s\s*/, '').replace(/\s\s*$/, ''); };

Atrox = {

    json_safe_parse: function (j) {
        return j ? JSON.parse(j) : null;
    },

    login: function (email, password, success, fail) {
        Api.login(email, password,
            function (auth_response) {
                success(auth_response);
                sessionStorage.setItem('atrox_session', auth_response.sessionId);
                Atrox.set_account(auth_response.account);
            }, fail);
    },

    register: function (email, success, fail) {
        Api.register(email,
            function (auth_response) {
                success(auth_response);
                sessionStorage.setItem('atrox_session', auth_response.sessionId);
                Atrox.set_account(auth_response.account);
            }, fail);
    },

    logout: function () {
        sessionStorage.clear();
    },

    account: function () {
        return Atrox.json_safe_parse(sessionStorage.getItem('atrox_account'));
    },

    set_account: function (account) {
        sessionStorage.setItem('atrox_account', JSON.stringify(account));
    },

    get_session: function (name, default_value) {
        if (sessionStorage.getItem(name)) return sessionStorage.getItem(name);
        return default_value;
    },
    set_session: function (name, value) { sessionStorage.setItem(name, value); }

};

