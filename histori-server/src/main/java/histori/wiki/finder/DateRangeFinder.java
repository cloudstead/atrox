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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor @Accessors(chain=true)
public class DateRangeFinder extends WikiDataFinderBase<TimeRange> {

    public static final String ATTR_DATE = "date";

    @Getter @Setter private String attrName = ATTR_DATE;

    public DateRangeFinder(WikiArchive wiki) { super(wiki); }
    public DateRangeFinder(WikiArchive wiki, ParsedWikiArticle article) { super(wiki, article); }

    @Override public TimeRange find() {
        for (WikiNode box : article.getInfoboxes()) {
            if (!isDateInfoboxCandidate(box.getName())) continue;
            WikiNode dateAttr = box.findChildNamed(ATTR_DATE);
            if (dateAttr != null) {
                WikiNode startDateBox = dateAttr.findFirstInfoboxWithName("start date");
                if (startDateBox != null) {
                    List<WikiNode> dateBoxNodes = startDateBox.getChildren();
                    int numChildren = dateBoxNodes.size();
                    if (numChildren > 0) {
                        String dateString = dateBoxNodes.get(0).getName();
                        if (numChildren > 0) dateString += "-" + dateBoxNodes.get(1).getName();
                        if (numChildren > 1) dateString += "-" + dateBoxNodes.get(2).getName();
                        return WikiDateFormat.parse(dateString);
                    }
                }
                return WikiDateFormat.parse(dateAttr.findAllChildText());
            }
        }
        return null;
    }

    private final Set<String> DATE_BOX_CANDIDATES = new HashSet<>(Arrays.asList(
            normalizeInfoboxName("Infobox military conflict")
    ));
    private boolean isDateInfoboxCandidate(String name) {
        return DATE_BOX_CANDIDATES.contains(normalizeInfoboxName(name));
    }

}
