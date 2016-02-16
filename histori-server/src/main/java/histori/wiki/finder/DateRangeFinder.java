package histori.wiki.finder;

import histori.model.support.TimeRange;
import histori.wiki.WikiDateFormat;
import histori.wiki.WikiNode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(chain=true)
public class DateRangeFinder extends WikiDataFinderBase<TimeRange> {

    public static final String ATTR_DATE = "date";

    @Getter @Setter private String attrName = ATTR_DATE;

    @Override public TimeRange find() {
        for (WikiNode box : article.getInfoboxes()) {
            for (WikiNode child : box.getChildren()) {
                if (child.getName().equals(attrName)) return WikiDateFormat.parse(child.findAllText());
            }
        }
        return null;
    }

}
