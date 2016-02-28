package histori.wiki.finder;

import histori.model.support.TimeRange;
import histori.wiki.WikiDateFormat;
import histori.wiki.WikiNode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor @Accessors(chain=true) @Slf4j
public class DateRangeFinder extends FinderBase<TimeRange> {

    public static final String ATTR_DATE = "date";

    @Getter @Setter private String attrName = ATTR_DATE;

    @Override public TimeRange find() {
        final List<WikiNode> infoboxes = article.getInfoboxes();
        TimeRange dateAttr = findRange(infoboxes);
        if (dateAttr != null) return dateAttr;
        dateAttr = findRange(article.getInfoboxesRecursive());
        if (dateAttr != null) return dateAttr;
        return null;
    }

    public TimeRange findRange(List<WikiNode> infoboxes) {
        for (WikiNode box : infoboxes) {
            if (!isDateInfoboxCandidate(box.getName())) continue;
            WikiNode dateAttr = box.findChildNamed(ATTR_DATE);
            if (dateAttr != null) {

                final WikiNode startDateBox = dateAttr.findFirstInfoboxWithName("start date");
                if (startDateBox != null) {
                    final TimeRange range = fromStartDateInfobox(startDateBox);
                    if (range != null) return range;
                }

                return WikiDateFormat.parse(dateAttr.findAllChildText());
            }
        }
        return null;
    }

    public static TimeRange fromStartDateInfobox(WikiNode startDateBox) {
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
        return null;
    }

    private final Set<String> DATE_BOX_CANDIDATES = new HashSet<>(Arrays.asList(
            normalizeInfoboxName("Infobox military conflict")
    ));
    private boolean isDateInfoboxCandidate(String name) {
        return DATE_BOX_CANDIDATES.contains(normalizeInfoboxName(name));
    }

}
