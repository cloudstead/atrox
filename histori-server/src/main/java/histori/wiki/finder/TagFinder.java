package histori.wiki.finder;

import histori.model.NexusTag;
import histori.wiki.ParsedWikiArticle;

import java.util.List;

public interface TagFinder extends WikiDataFinder<List<NexusTag>> {

    public void setArticle (ParsedWikiArticle article);

}
