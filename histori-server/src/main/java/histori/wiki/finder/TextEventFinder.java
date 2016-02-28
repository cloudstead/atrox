package histori.wiki.finder;

import histori.model.support.TimeRange;
import histori.wiki.WikiDateFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
public class TextEventFinder {

    public static final String DATE_GROUP = "([^\\.]*\\d{4}"+WikiDateFormat.ERA_GROUP +")";

    public static final TextEventFinder[] FINDERS = new TextEventFinder[] {
            new TextEventFinder("first\\s+.*?settlers?.+?arrived\\s+(?:in|on)\\s+"+ DATE_GROUP, "settled"),
            new TextEventFinder("was formed\\s+(?:in|on)\\s+"+ DATE_GROUP, "formed"),
            new TextEventFinder("founded\\s+(?:by\\s+.+?\\s+)?in\\s+the\\s+(.+?\\s+century)", "founded"),
            new TextEventFinder("in\\s+"+DATE_GROUP+"\\s.+?(:?formed by|founded by)", "founded")
    };

    @Getter @Setter private String regex;
    @Getter @Setter private String description;

    @Getter(lazy=true) private final Pattern pattern = initPattern();
    private Pattern initPattern() { return Pattern.compile(regex, Pattern.CASE_INSENSITIVE|Pattern.MULTILINE); }

    public TimeRange findRange (String paragraph) {
        final Matcher matcher = getPattern().matcher(paragraph);
        if (matcher.find()) {
            return WikiDateFormat.parse(matcher.group(1));
        }
        return null;
    }

    public static TextEventFinderResult find(String paragraph) {
        TimeRange range = null;
        TextEventFinder matched = null;
        for (TextEventFinder finder : FINDERS) {
            range = finder.findRange(paragraph);
            if (range != null) {
                matched = finder;
                break;
            }
        }
        return range == null ? null : new TextEventFinderResult(matched, range);
    }

}
