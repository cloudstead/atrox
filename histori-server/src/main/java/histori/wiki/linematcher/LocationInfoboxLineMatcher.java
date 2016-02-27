package histori.wiki.linematcher;

import histori.wiki.finder.InfoboxNames;

public class LocationInfoboxLineMatcher implements LineMatcher {

    @Override public boolean matches(String line) { return InfoboxNames.lineMatchesLocationInfobox(line); }

}
