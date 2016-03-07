
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
    // todo: handle errors?
    Histori.get_bookmarks(function (data) {
        buildBookmarksForm(data);
    });
}

var bookmark_anonymous_warning_html = '';
function buildBookmarksForm (bookmarks) {
    var bookmarksList = $('#bookmarksList');
    bookmarksList.empty();

    function display_bounds(bounds) {
        return bounds.south.toFixed(2)+'&#176;S, '+bounds.west.toFixed(2)+'&#176;W to<br/>'
            + bounds.north.toFixed(2)+'&#176;N, '+bounds.east.toFixed(2)+'&#176;E';
    }

    function display_range(range) {
        var start = slider.label_for_raw(parseFloat(range.start));
        var end = slider.label_for_raw(parseFloat(range.end));
        return start + ' to ' + end;
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
        var bookmarkLinkCell = $('<td nowrap class="bookmark_info"></td>');
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
        var rangeCell = $('<td nowrap class="bookmark_info">'+display_range(range)+'</td>');
        bookmarkRow.append(rangeCell);

        // query count cell
        var queryCell = $('<td class="bookmark_info" align="center"></td>');
        for (var j=0; j<bookmarks[i].state.searches.length; j++) {
            queryCell.append($('<img title="'+bookmarks[i].state.searches[j].query.escape()+'" src="'+bookmarks[i].state.searches[j].icon+'"/>'));
        }
        bookmarkRow.append(queryCell);

        // bookmark buttons
        if (!isAnonymous()) {
            // only logged-in users can create permalinks
            var permalinkCell = $('<td valign="middle"></td>');
            var permalinkButton = $('<button class="permalinkBookmarkIcon" title="share / permalink"><img src="iconic/png/share.png"/></button>');
            permalinkButton.on('click', permalink_button_click_handler(bookmarks[i].name));
            permalinkCell.append(permalinkButton);
            bookmarkRow.append(permalinkCell);
        }

        var removeButtonCell = $('<td valign="middle"></td>');
        var removeButton = $('<button><img title="remove" class="removeBookmarkIcon" src="iconic/png/x.png"/></button>');
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
    $('.bookmarkCurrentSearchIcon').remove();
    for (var j=0; j<state.searches.length; j++) {
        queryCell.append($('<img class="bookmarkCurrentSearchIcon" title="'+state.searches[j].query.escape()+'" src="'+state.searches[j].icon+'"/>'));
    }
    queryCell.attr('title', bookmark_query_tooltip(state.searches));

    var anonWarning = $('#bookmark_anonymous_warning');
    if (isAnonymous()) {
        if (anonWarning.html().length == 0) anonWarning.html(bookmark_anonymous_warning_html);
        anonWarning.css('visibility', 'visible');
    } else {
        bookmark_anonymous_warning_html = anonWarning.html();
        anonWarning.html('');
        anonWarning.css('visibility', 'hidden');
    }
    var overwriteBookmark = $('#btnOverwriteBookmark');
    if (overwriteBookmark.val() == 'cancel') {
        overwriteBookmark.val('overwrite existing');
    }
    if (bookmarks.length == 0) {
        overwriteBookmark.css('visibility', 'hidden');
    } else {
        overwriteBookmark.css('visibility', 'visible');
    }
    showForm('bookmarksContainer');
    bookmarkName.focus();
}

function permalink_button_click_handler(name) {
    return function (e) {
        Api.make_permalink(name, function(data) {
            window.location.href = location.protocol + '//' + location.host + location.pathname + '?link=' + data.name;
        });
    }
}

function restore_bookmark_state_click_handler (name) {
    return function (e) { Histori.restore_bookmark_state(name); return false; }
}

function overwrite_bookmark_click_handler (name) {
    return function (e) {
        Histori.overwrite_bookmark(name,
            // success
            function () {
                closeBookmarks();
                showBookmarks();
            });
        return false;
    }
}

function remove_bookmark_click_handler (name) {
    return function (e) {
        Histori.remove_bookmark(name, function (uuid) {
            $('#bookmark_'+uuid).remove();
            var overwriteBookmark = $('#btnOverwriteBookmark');
            if ($('.bookmark_row').length == 0) {
                overwriteBookmark.css('visibility', 'hidden');
            } else {
                overwriteBookmark.css('visibility', 'visible');
            }
        });
        return false;
    }
}

function save_bookmark (name) {
    Histori.add_bookmark(name,
        // success
        function () {
            closeBookmarks();
            showBookmarks();
        },
        // failure
        function () {
            $('#bookmark_name').css({
                border: '2px solid red'
            });
        });
}

function toggle_overwrite_bookmark() {
    var button = $('#btnOverwriteBookmark');
    var i, jqRow;
    var rows = $('.bookmark_row');
    if (button.val() == 'overwrite existing') {
        button.val('cancel');
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
        button.val('overwrite existing');
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