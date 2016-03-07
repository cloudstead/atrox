
function showLoginForm () {

    var anonymous = isAnonymous();
    if (anonymous) {
        showForm('loginContainer');
        $('#login_email_field').focus();
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

    function display_bounds(bounds) {
        return bounds.south.toFixed(2)+'&#176;S, '+bounds.west.toFixed(2)+'&#176;W to<br/>'
             + bounds.north.toFixed(2)+'&#176;N, '+bounds.east.toFixed(2)+'&#176;E';
    }

    function display_range(range) {
        var start = slider.label_for_slider_value(range.start);
        var end = slider.label_for_slider_value(range.end);
        return start+' to ' + end;
    }

    function bookmark_query_tooltip(searches) {
        var queries = '';
        for (var j = 0; j <searches.length; j++) {
            if (queries.length > 0) queries += '|';
            queries += searches[j].query.escape();
        }
        return queries;
    }

    for (var i=0; i<bookmarks.length; i++) {
        var bookmarkRow = $('<tr class="bookmark_row" id="bookmark_'+bookmarks[i].uuid+'"></tr>');

        // Create bookmark link - clicking restores bookmark state
        var bookmarkLinkCell = $('<td class="bookmark_info"></td>');
        var bookmarkLink = $('<a class="bookmark_link" href=".">'+bookmarks[i].name+'</a>');
        bookmarkLink.on('click', restore_bookmark_state_click_handler(bookmarks[i].name));

        // overwrite button, only shown when main 'overwrite' button is toggled
        var overwriteButton = $('<button class="overwrite_bookmark_button">save</button>');
        overwriteButton.css('visibility', 'hidden');
        overwriteButton.on('click', overwrite_bookmark_click_handler(bookmarks[i].name));

        bookmarkLinkCell.append(bookmarkLink).append(overwriteButton);
        bookmarkRow.append(bookmarkLinkCell);

        // bounds cell
        var bounds = bookmarks[i].state.map;
        var boundsCell = $('<td class="bookmark_info"></td>');
        boundsCell.append(display_bounds(bounds));
        bookmarkRow.append(boundsCell);

        // time cell
        var range = bookmarks[i].state.timeline.range;
        var rangeCell = $('<td class="bookmark_info">'+display_range(range)+'</td>');
        bookmarkRow.append(rangeCell);

        // query count cell
        var queryCountCell = $('<td class="bookmark_info" align="center"></td>');
        queryCountCell.addClass('replace_jq_tooltip_linebreaks');
        queryCountCell.html(bookmarks[i].state.searches.length);
        queryCountCell.attr('title', bookmark_query_tooltip(bookmarks[i].state.searches));
        bookmarkRow.append(queryCountCell);

        // Delete bookmark button
        var removeButtonCell = $('<td></td>');
        var removeButton = $('<button><img class="removeBookmarkIcon" src="iconic/png/x.png"/></button>');
        removeButton.on('click', remove_bookmark_click_handler(bookmarks[i].name));
        removeButtonCell.append(removeButton);
        bookmarkRow.append(removeButtonCell);
        bookmarksList.append(bookmarkRow);
    }

    // populate "new bookmark" row
    var state = Histori.get_session_state();
    var bookmarkName = $('#bookmark_name');
    bookmarkName.val($('.searchBox_query')[0].value); // use first search query as name field
    bookmarkName.keydown(function (e) { $('#bookmark_name').css('border', '0') });
    $('#bookmark_bounds').html(display_bounds(state.map));
    $('#bookmark_range').html(display_range(state.timeline.range));

    var queryCell = $('#bookmark_query_count');
    queryCell.addClass('replace_jq_tooltip_linebreaks');
    queryCell.html(state.searches.length);
    queryCell.attr('title', bookmark_query_tooltip(state.searches));

    if (isAnonymous()) {
        $('#bookmark_anonymous_warning').css('visibility', 'visible');
    } else {
        $('#bookmark_anonymous_warning').css('visibility', 'hidden');
    }
    var overwriteBookmark = $('#btnOverwriteBookmark');
    if (overwriteBookmark.html() == 'cancel') {
        overwriteBookmark.html('overwrite existing');
    }
    showForm('bookmarksContainer');
    bookmarkName.focus();
}

function restore_bookmark_state_click_handler (uuid) {
    return function (e) { Histori.restore_bookmark_state(uuid); return false; }
}

function overwrite_bookmark_click_handler (name) {
    return function (e) {
        if (Histori.overwrite_bookmark(name)) {
            closeBookmarks();
            showBookmarks();
        }
        return false;
    }
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
        showBookmarks();
    } else {
        $('#bookmark_name').css({
            border: '2px solid red'
        });
    }
}

function toggle_overwrite_bookmark() {
    var button = $('#btnOverwriteBookmark');
    var i, jqRow;
    var rows = $('.bookmark_row');
    if (button.html() == 'overwrite existing') {
        button.html('cancel');
        for (i=0; i<rows.length; i++) {
            jqRow = $(rows[i]);
            //jqRow.find('.bookmark_link').css('visibility', 'hidden');
            jqRow.find('.overwrite_bookmark_button').css('visibility', 'visible');
        }
    } else {
        // hide all instances of button class, make links visible
        for (i=0; i<rows.length; i++) {
            jqRow = $(rows[i]);
            //jqRow.find('.bookmark_link').css('visibility', 'visible');
            jqRow.find('.overwrite_bookmark_button').css('visibility', 'hidden');
        }
        button.html('overwrite existing');
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