package atrox.dao;

import atrox.model.WorldEvent;
import org.springframework.stereotype.Repository;

import static atrox.ApiConstants.BOUND_RANGE;
import static atrox.ApiConstants.RANGE_SEP;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;

@Repository public class WorldEventDAO extends CanonicallyNamedEntityDAO<WorldEvent> {

    @Override protected String formatBound(String entityAlias, String bound, String value) {
        switch (bound) {
            case BOUND_RANGE:
                final int sepPos = value.indexOf(RANGE_SEP);
                if (sepPos == -1 || sepPos == value.length()-1) die("formatBound("+BOUND_RANGE+"): invalid value: "+value);
                final String start = value.substring(0, sepPos);
                final String end = value.substring(sepPos+1);
                return "(x.endPoint.instant   >= "+start+" AND x.endPoint.instant   <= "+end+") "
                  + "OR (x.startPoint.instant >= "+start+" AND x.startPoint.instant <= "+end+")";
        }
        return notSupported("Invalid bound: " + bound);
    }

}
