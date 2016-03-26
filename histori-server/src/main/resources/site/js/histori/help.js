
function showHelp () {
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
        height: '100%',
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