package histori.wiki.finder;

import histori.wiki.ParsedWikiArticle;

public class TagFinderFactory {

    public static TagFinder build(ParsedWikiArticle parsed) {
        final TagFinder finder = _build(parsed);
        if (finder != null) finder.setArticle(parsed);
        return finder;
    }

    private static TagFinder _build(ParsedWikiArticle parsed) {
        if (parsed.getName().toLowerCase().contains("battle of")) {
            return new BattleTagFinder();
        }
        return null;
    }

}
