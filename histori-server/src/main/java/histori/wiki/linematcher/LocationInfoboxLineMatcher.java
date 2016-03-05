package histori.wiki.linematcher;

import histori.wiki.finder.InfoboxNames;

public class LocationInfoboxLineMatcher extends LineMatcherBase {

    @Override public boolean matches(String line) { return InfoboxNames.lineMatchesLocationInfobox(line); }

}
