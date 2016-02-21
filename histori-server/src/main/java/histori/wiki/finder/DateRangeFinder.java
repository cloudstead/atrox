package histori.wiki.finder;

import histori.model.support.TimeRange;
import histori.wiki.ParsedWikiArticle;
import histori.wiki.WikiArchive;
import histori.wiki.WikiDateFormat;
import histori.wiki.WikiNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @Accessors(chain=true)
public class DateRangeFinder extends WikiDataFinderBase<TimeRange> {

    public static final String ATTR_DATE = "date";

    @Getter @Setter private String attrName = ATTR_DATE;

    public DateRangeFinder(WikiArchive wiki) { super(wiki); }
    public DateRangeFinder(WikiArchive wiki, ParsedWikiArticle article) { super(wiki, article); }

    @Override public TimeRange find() {
        for (WikiNode box : article.getInfoboxes()) {
            if (isIgnoredInfobox(box)) continue;
            WikiNode dateAttr = box.findChildNamed(ATTR_DATE);
            if (dateAttr != null) return WikiDateFormat.parse(dateAttr.findAllChildText());
        }
        return null;
    }

}
