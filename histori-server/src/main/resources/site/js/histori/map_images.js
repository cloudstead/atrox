function closeMapImages () {
    var container = $('#mapImageContainer');
    container.css('zIndex', -1);
}

var map_image_mode = 'image';
function toggleMapImageMode() {
    if (map_image_mode == 'image') {
        map_image_mode = 'map';
        $('#mapImg').css({'pointer-events': 'none'});
        $('.container').css({'pointer-events': 'none'});
        $('#btnMapImageMode').html('unfreeze image');
    } else {
        map_image_mode = 'image';
        $('#mapImg').css({'pointer-events': 'visible'});
        $('.container').css({'pointer-events': 'visible'});
        $('#btnMapImageMode').html('freeze image');
    }
}

// dummy-noop at first
var rotate_handler = window.setInterval(function () {}, 5000);

function createRotationHandler (id, direction) {
    //console.log("ONMOUSEDOWN: createRotationHandler: creating for "+id+"/"+direction);
    var element = $('#'+id);

    return function (e) {
        Histori.set_session(id+"_rotate_start", Date.now());

        window.clearInterval(rotate_handler);
        rotate_handler = window.setInterval(function () {
            var degree = parseFloat(Histori.get_session(id+"_rotate_amount", 0.0));
            if (degree == null) degree = 0.0;
            Histori.set_session(id+"_rotate_amount", degree);

            var start = parseInt(Histori.get_session(id+"_rotate_start", Date.now()));
            if (start == null || isNaN(start)) start = Date.now();
            Histori.set_session(id+"_rotate_start", start);

            var duration = parseInt(Date.now() - start);
            if (duration > 15000) {
                window.clearInterval(rotate_handler);
                rotate_handler = window.setInterval(function () {}, 5000);
            }

            var delta = Math.min(5.0, 50.0 * Math.pow(duration/10000.0, 3));
            delta *= (1.0) * direction;
            degree += delta;
            Histori.set_session(id+"_rotate_amount", degree);

            console.log("adjusted "+id+" angle by delta="+delta+", now="+degree+" (duration="+duration+")");

            element.css({ WebkitTransform: 'rotate(' + degree + 'deg)'});
            element.css({ '-moz-transform': 'rotate(' + degree + 'deg)'});

        }, 50);

        // Using return false prevents browser's default,
        // often unwanted mousemove actions (drag & drop)
        return false;
    }
}
