package histori.wiki.finder;

import histori.wiki.ParsedWikiArticle;
import histori.wiki.WikiArchive;
import histori.wiki.WikiNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public abstract class WikiDataFinderBase<T> implements WikiDataFinder<T> {

    private static final List<String> IGNORED_INFOBOXES = new ArrayList<>();
    static {
        for (String s : IGNORED_INFOBOX_NAMES) {
            IGNORED_INFOBOXES.add(normalizeInfoboxName(s));
        }
    }
    private static String normalizeInfoboxName(String s) { return s.toLowerCase().replaceAll("\\s", ""); }

    @Getter @Setter protected WikiArchive wiki;
    @Getter @Setter protected ParsedWikiArticle article;

    public WikiDataFinderBase (WikiArchive wiki) { this.wiki = wiki; }

    public static boolean isIgnoredInfobox(WikiNode box) {
        return IGNORED_INFOBOXES.contains(normalizeInfoboxName(box.getName()));
    }
}
