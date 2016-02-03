
String.prototype.trim = String.prototype.trim || function trim() { return this.replace(/^\s\s*/, '').replace(/\s\s*$/, ''); };

Atrox = {

    json_safe_parse: function (j) {
        return j ? JSON.parse(j) : null;
    },

    login: function (email, password) {
        var auth_response = Api.login(email, password);
        sessionStorage.setItem('atrox_session', auth_response.sessionId);
        Atrox.set_account(auth_response.account);
    },

    logout: function () {
        sessionStorage.clear();
    },

    account: function () {
        return Atrox.json_safe_parse(sessionStorage.getItem('atrox_account'));
    },

    set_account: function (account) {
        sessionStorage.setItem('atrox_account', JSON.stringify(account));
    }

};

