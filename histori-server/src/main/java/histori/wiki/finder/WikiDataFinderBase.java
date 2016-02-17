package histori.wiki.finder;

import histori.wiki.ParsedWikiArticle;
import histori.wiki.WikiArchive;
import histori.wiki.WikiNode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain=true)
public abstract class WikiDataFinderBase<T> implements WikiDataFinder<T> {

    @Getter @Setter protected WikiArchive wiki;
    @Getter @Setter protected ParsedWikiArticle article;

    public static boolean isIgnoredInfobox(WikiNode box) {
        return box.getName().equalsIgnoreCase(INFOBOX_REFIMPROVE);
    }
}
