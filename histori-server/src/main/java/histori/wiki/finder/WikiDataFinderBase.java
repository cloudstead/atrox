package histori.wiki.finder;

import histori.wiki.ParsedWikiArticle;
import histori.wiki.WikiArchive;
import histori.wiki.WikiNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public abstract class WikiDataFinderBase<T> implements WikiDataFinder<T> {

    @Getter @Setter protected WikiArchive wiki;
    @Getter @Setter protected ParsedWikiArticle article;

    public WikiDataFinderBase (WikiArchive wiki) { this.wiki = wiki; }

    public static boolean isIgnoredInfobox(WikiNode box) {
        final String name = box.getName();
        return name.equalsIgnoreCase(INFOBOX_REFIMPROVE)
                || name.equalsIgnoreCase(INFOBOX_COPYPASTE)
                || name.equalsIgnoreCase(INFOBOX_NO_FOOTNOTES);
    }
}
