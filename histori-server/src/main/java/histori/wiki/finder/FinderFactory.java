package histori.wiki.finder;

import histori.wiki.ParsedWikiArticle;
import histori.wiki.WikiArchive;
import histori.wiki.finder.impl.ConflictFinder;
import histori.wiki.finder.impl.MetroFinder;
import histori.wiki.matcher.LocationInfoboxMatcher;

import static histori.wiki.finder.InfoboxNames.INFOBOX_MILITARY_CONFLICT;

public class FinderFactory {

    public static final LocationInfoboxMatcher LOCATION_MATCHER = new LocationInfoboxMatcher();

    public static WikiDataFinder build(WikiArchive wiki, ParsedWikiArticle parsed) {
        final WikiDataFinder finder = _build(parsed);
        if (finder != null) {
            finder.setArticle(parsed);
            finder.setWiki(wiki);
        }
        return finder;
    }

    private static WikiDataFinder _build(ParsedWikiArticle parsed) {
        if (parsed.findFirstInfoboxWithName(INFOBOX_MILITARY_CONFLICT) != null) {
            return new ConflictFinder();
        } else if (parsed.findFirstInfoboxMatch(LOCATION_MATCHER) != null) {
            return new MetroFinder();
        }
        return null;
    }

}
