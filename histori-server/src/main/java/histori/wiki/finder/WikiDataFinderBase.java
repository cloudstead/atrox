package histori.wiki.finder;

import histori.wiki.ParsedWikiArticle;
import histori.wiki.WikiArchive;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain=true)
public abstract class WikiDataFinderBase<T> implements WikiDataFinder<T> {

    @Getter @Setter protected WikiArchive wiki;
    @Getter @Setter protected ParsedWikiArticle article;

}
