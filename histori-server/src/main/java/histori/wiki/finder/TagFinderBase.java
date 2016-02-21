package histori.wiki.finder;

import histori.model.NexusTag;
import histori.wiki.ParsedWikiArticle;
import lombok.Getter;
import lombok.Setter;

public abstract class TagFinderBase implements TagFinder {

    @Getter @Setter protected ParsedWikiArticle article;

    public NexusTag newTag(String tagName, String tagType) {
        NexusTag tag;

        // chop anything after a trailing HTML-encoded <
        int openTagPos = tagName.indexOf("&lt;");
        if (openTagPos != -1) tagName = tagName.substring(0, openTagPos);
        tag = new NexusTag();
        tag.setTagName(tagName);
        tag.setTagType(tagType);
        return tag;
    }

    public NexusTag newTag(String tagName, String tagType, String field, String value) {
        NexusTag tag = newTag(tagName, tagType);
        tag.setValue(field, value);
        return tag;
    }

}
