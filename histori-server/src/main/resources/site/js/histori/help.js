function showHelp (showQuick) {
    if (typeof showQuick != "undefined" && showQuick == false) {
        $('#btn_quickHelp').css('visibility', 'hidden');
    }
    var helpContainer = $('#helpContainer');
    var quickHelpLink = $('#helpContainer_quickHelp');
    var hcWidth = helpContainer.outerWidth();
    quickHelpLink.css({
        top: -50,
        left: hcWidth - 100
    });
    var topicsDiv = $('#helpContainer_topics');
    var helpIframeDiv = $('#helpContainer_iframe');
    helpIframeDiv.css({
        top: 0 - topicsDiv.outerHeight() - 20,
        left: topicsDiv.outerWidth() + 20,
        'overflow-y': 'auto'
    });
    showForm('helpContainer');
}

function showQuickHelp () {
    $(".quick-help").each(function (index) {
        var targetElement = this.id.substring(this.id.indexOf('_')+1);
        var positionType = this.id.substring(0, this.id.indexOf('_'));
        var target = $('#'+targetElement);
        switch (positionType) {
            default: case 'help-top-left':
                $(this).css({
                    position: 'absolute',
                    top: target.position().top - $(this).outerHeight(),
                    left: target.position().left - 5
                });
                break;
            case 'help-top-right':
                $(this).css({
                    position: 'absolute',
                    top: target.position().top - $(this).outerHeight() - 5,
                    left: target.position().left + target.outerWidth() + 5
                });
                break;
            case 'help-bottom-left':
                $(this).css({
                    position: 'absolute',
                    top: target.position().top + target.outerHeight() + 5,
                    left: target.position().left - 5
                });
                break;
            case 'help-bottom-right':
                $(this).css({
                    position: 'absolute',
                    top: target.position().top + target.outerHeight() + 5,
                    left: target.position().left + target.outerWidth() + 5
                });
                break;
            case 'help-center':
                $(this).center();
                break;
        }
        $(this).css('visibility', 'visible');
    });
    closeForm();
}

function resizeHeight () {
    if (window.parent != window) {
        var eof = $('#EOF');
        var top = eof.offset().top;
        $('#helpContainer_iframe', window.parent.document).height(top);
        $('#helpIframe', window.parent.document).height(top);
        $('a').each(function () {
            var element = $(this);
            if ((typeof element.attr('class') == "undefined") || (element.attr('class') != 'no_change_target')) {
                element.attr('target', '_blank');
            }
        });
    } else {
        $('body').css({overflow: 'scroll', 'padding-left': '50px', 'padding-right': '50px'});
    }
}

$(function () {
    // Set help topic link attributes: make link target help frame, auto-resize frame
    $('.helpTopic').each(function () {
        var element = $(this);
        if (typeof element.attr('target') == "undefined") element.attr('target', 'helpFrame');
    });
    var selectTopic = getParameterByName("help");
    if (typeof selectTopic != "undefined" && selectTopic != null) {
        if (selectTopic.indexOf(':') != -1) return; // sanity check
        var qPos = selectTopic.indexOf('?');
        var query = (qPos == -1) ? '' : decodeURIComponent(selectTopic.substring(qPos+1));
        if (qPos != -1) selectTopic = selectTopic.substring(0, qPos);
        var helpUrl = '/help/'+selectTopic+'.html?'+query;
        console.log('helpUrl='+helpUrl);
        $('#helpIframe').attr('src', helpUrl);
        showHelp();
    }
});
