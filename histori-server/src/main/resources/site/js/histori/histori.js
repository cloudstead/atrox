
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
    for (var i=1; i<MONTH_SHORT_NAMES.length; i++) {
        if (val == MONTH_SHORT_NAMES[i]) return i;
    }
    for (var i=1; i<MONTHS.length; i++) {
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
    if (typeof id != "undefined" && id != null) {
        var container = $('#' + id);
        container.css('z-index', -1);
    }
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
                captcha: fields['g-recaptcha-response'].value
            },
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

    update_account: function (fields, success, fail) {
        Api.update_account({
            name: fields['name'].value,
            email: fields['email'].value,
            currentPassword: fields['currentPassword'].value,
            newPassword: fields['newPassword'].value,
            subscribe: fields['subscribe'].checked
        }, success, fail);
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

