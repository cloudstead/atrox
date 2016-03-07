
function showLoginForm () {

    var anonymous = isAnonymous();
    if (anonymous) {
        showForm('loginContainer');
    } else {
        showAccountForm();
    }
}
function showAccountForm () {
    var account = Histori.account();
    var accountContainer = $('#accountContainer');
    accountContainer.find('input[name="name"]').val(account.name);
    accountContainer.find('input[name="email"]').val(account.email);
    accountContainer.find('input[name="subscribe"]').prop('checked', (typeof account.subscriber != 'undefined') && account.subscriber);
    accountContainer.find('input[name="currentPassword"]').val('');
    accountContainer.find('input[name="newPassword"]').val('');
    showForm('accountContainer');
}
function showRegForm () {
    // todo: how to disable tooltip on recaptcha?
    showForm('regContainer');
}
function showForgotPassForm () { showForm('forgotPassContainer'); }
function showResetPassForm () { showForm('resetPassContainer'); }

function showBookmarks () {
    var bookmarks = Histori.get_bookmarks();
    var bookmarksList = $('#bookmarksList');
    bookmarksList.empty();
    for (var i=0; i<bookmarks.length; i++) {
        var bookmarkRow = $('<tr id="bookmark_'+bookmarks[i].uuid+'"></tr>');

        // Create bookmark link - clicking restores bookmark state
        var bookmarkLinkCell = $('<td colspan="2"></td>');
        var bookmarkLink = $('<a href=".">'+bookmarks[i].name+'</a>');
        bookmarkLink.on('click', restore_bookmark_state_click_handler(bookmarks[i].uuid));
        bookmarkLinkCell.append(bookmarkLink);
        bookmarkRow.append(bookmarkLinkCell);

        // Create remove button to delete bookmark
        var removeButtonCell = $('<td></td>');
        var removeButton = $('<button><img class="removeBookmarkIcon" src="iconic/png/x.png"/></button>');
        removeButton.on('click', remove_bookmark_click_handler(bookmarks[i].name));
        removeButtonCell.append(removeButton);
        bookmarkRow.append(removeButtonCell);
        bookmarksList.append(bookmarkRow);
    }
    if (bookmarks.length > 0) {
        $('#savedBookmarksHeader').css('visibility', 'visible');
    } else {
        $('#savedBookmarksHeader').css('visibility', 'hidden');
    }

    // populate bookmark name with first search
    $('#bookmark_name').val($('.searchBox_query')[0].value);

    showForm('bookmarksContainer');
}

function restore_bookmark_state_click_handler (uuid) {
    return function (e) { Histori.restore_bookmark_state(uuid); return false; }
}

function remove_bookmark_click_handler (name) {
    return function (e) {
        var removed = Histori.remove_bookmark(name);
        if (removed != null) {
            $('#bookmark_'+removed.uuid).remove();
        }
        return false;
    }
}

function save_bookmark (name) {
    if (Histori.add_bookmark(name)) {
        closeBookmarks();
    } else {

    }
}

function closeLoginForm () { closeForm('loginContainer'); }
function closeRegForm () { closeForm('regContainer'); }
function closeForgotPassForm () { closeForm('forgotPassContainer'); }
function closeResetPassForm () { closeForm('resetPassContainer'); }
function closeBookmarks () { closeForm('bookmarksContainer'); }

function clearAccountFormErrors() {
    $('#accountContainer').find('input').css('border', '');
}
function closeAccountForm () {
    clearAccountFormErrors();
    closeForm('accountContainer');
}

function successfulLogin () {
    closeLoginForm();
}

function successfulPasswordReset () {
    showLoginForm();
    var authError = $(".authError");
    authError.css('color', 'green');
    authError.html('Your password was successfully updated');
}
function successfulForgotPassword () {
    var authError = $(".authError");
    authError.css('color', 'green');
    authError.html('We sent you an email to reset your password');
}

function successfulRegistration (data) {
    closeRegForm();
}

function successfulAccountUpdate (data) {
    if (typeof data != "undefined") {
        Histori.set_account(data);
        closeAccountForm();
    }
}

function handleAuthError (authType) {
    return function (jqXHR, status, error) {

        if (jqXHR.status == 200) {
            console.log('not an error: '+jqXHR.status);
            return;
        }

        var authError = $(".authError");
        authError.css('color', 'red');
        if (jqXHR.status == 404) {
            authError.html("account not found");

        } else if (jqXHR.status == 422) {
            if (typeof jqXHR.responseJSON != "undefined" && is_array(jqXHR.responseJSON)) {
                var msg = '';
                for (var i=0; i<jqXHR.responseJSON.length; i++) {
                    if (msg.length > 0) msg += '<br/>';
                    if (typeof jqXHR.responseJSON[i].message != "undefined") {
                        msg += jqXHR.responseJSON[i].message;
                    } else {
                        msg += jqXHR.responseJSON[i].messageTemplate;
                    }
                }
            }
            authError.html(msg);

        } else if (authType == 'login') {
            authError.html("login error");

        } else {
            authError.html("authentication error");
        }
    }
}

function validateAccountForm (form) {

    var ok = true;
    var authError = $(".authError");
    authError.empty();
    authError.css('color', 'red');

    var currentPassword = form.elements['currentPassword'].value;
    var password = form.elements['newPassword'].value;
    if (password.length > 0 && currentPassword.length == 0) {
        $('#accountContainer').find('input[name="currentPassword"]').css('border', '2px solid red');
        ok = false;

    } else if (password.length == 0 && currentPassword.length > 0) {
        $('#accountContainer').find('input[name="newPassword"]').css('border', '2px solid red');
        ok = false;
    }
    if (form.elements['name'].value.length == 0) {
        $('#accountContainer').find('input[name="name"]').css('border', '2px solid red');
        ok = false;
    }
    if (form.elements['email'].value.length == 0) {
        $('#accountContainer').find('input[name="email"]').css('border', '2px solid red');
        ok = false;
    }
    return ok;
}