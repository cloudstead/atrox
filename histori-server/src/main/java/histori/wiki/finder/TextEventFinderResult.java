package histori.wiki.finder;

import histori.model.support.TimeRange;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class TextEventFinderResult {

    @Getter @Setter TextEventFinder matched;
    @Getter @Setter TimeRange range;

    public String getDescription () { return matched.getDescription(); }
}
