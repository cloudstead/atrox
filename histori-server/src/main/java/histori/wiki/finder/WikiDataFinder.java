package histori.wiki.finder;

import histori.wiki.ParsedWikiArticle;
import histori.wiki.WikiArchive;

public interface WikiDataFinder<T> {

    public WikiDataFinder setWiki(WikiArchive wiki);

    public WikiDataFinder setArticle (ParsedWikiArticle article);

    public T find ();
}
