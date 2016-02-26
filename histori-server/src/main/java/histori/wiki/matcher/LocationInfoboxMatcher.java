package histori.wiki.matcher;

import histori.wiki.WikiNode;
import histori.wiki.finder.InfoboxNames;

public class LocationInfoboxMatcher extends InfoboxMatcher {

    @Override protected boolean infoboxMatches(WikiNode node) { return InfoboxNames.isLocationInfobox(node.getName()); }

}
