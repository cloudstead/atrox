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

    @Getter @Setter private String regex;
    @Getter @Setter private String description;

    @Getter(lazy=true) private final Pattern pattern = initPattern();
    private Pattern initPattern() { return Pattern.compile(regex, Pattern.CASE_INSENSITIVE|Pattern.MULTILINE); }

    public TimeRange find (String paragraph) {

        final Matcher matcher = getPattern().matcher(paragraph);
        if (matcher.find()) {
            return WikiDateFormat.parse(matcher.group(1));
        }

        return null;
    }

}
