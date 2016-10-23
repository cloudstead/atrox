package histori.wiki;

import histori.model.support.TimePoint;
import histori.model.support.TimeRange;
import lombok.Getter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.get;
import static org.cobbzilla.util.reflect.ReflectionUtil.set;
import static org.cobbzilla.util.string.StringUtil.uncapitalize;

public class WikiDateFormat {


    // the second entry in each array is the field mask, tells us which fields we can use from the joda-time
    private static final String[][] FORMAT_BASES = {
            {"yyyy", "y"},

            {"yyyy-MM-dd", "yMd"},
            {"dd MMM yyyy", "yMd"},
            {"dd MMMM yyyy", "yMd"},
            {"d MMM yyyy", "yMd"},
            {"d MMMM yyyy", "yMd"},
            {"MMM yyyy", "yM"},
            {"MMM, yyyy", "yM"},
            {"MMMM yyyy", "yM"},
            {"MMMM, yyyy", "yM"},
            {"MMMM dd yyyy", "yMd"},
            {"MMMM dd, yyyy", "yMd"},
            {"MMMM d yyyy", "yMd"},
            {"MMMM d, yyyy", "yMd"},

            {"'Summer' yyyy", "y"},
            {"'Fall' yyyy", "y"},
            {"'Autumn' yyyy", "y"},
            {"'Winter' yyyy", "y"},
            {"'Spring' yyyy", "y"},

            {"'Summer of' yyyy", "y"},
            {"'Fall of' yyyy", "y"},
            {"'Autumn of' yyyy", "y"},
            {"'Winter of' yyyy", "y"},
            {"'Spring of' yyyy", "y"},

            {"'Early' yyyy", "y"},
            {"'Late' yyyy", "y"},
            {"'Early' yyyy's'", "y"},
            {"'Late' yyyy's'", "y"},
            {"'Early' MMM yyyy", "yM"},
            {"'Late' MMM yyyy", "yM"},
            {"'Early' MMMM yyyy", "yM"},
            {"'Late' MMMM yyyy", "yM"},

            {"EE, d MMM yyyy HH:mm:ss", "yMdhms"},
            {"EE, dd MMM yyyy HH:mm:ss", "yMdhms"},
            {"EE, d MMM yyyy HH:mm:ss z", "yMdhms"},
            {"EE, dd MMM yyyy HH:mm:ss z", "yMdhms"},
            {"E, d MMM yyyy HH:mm:ss", "yMdhms"},
            {"E, dd MMM yyyy HH:mm:ss", "yMdhms"},
            {"E, d MMM yyyy HH:mm:ss z", "yMdhms"},
            {"E, dd MMM yyyy HH:mm:ss z", "yMdhms"},
            {"EEEE, MMM yyyy", "yM"},
            {"EEEE, MMMM yyyy", "yM"},
            {"EEEE, MMMM dd yyyy", "yMd"},
            {"EEEE, MMMM dd, yyyy", "yMd"},
            {"EEEE, MMMM d yyyy", "yMd"},
            {"EEEE, MMMM d, yyyy", "yMd"},
            {"E, MMM yyyy", "yM"},
            {"E, MMMM yyyy", "yM"},
            {"E, MMMM dd yyyy", "yMd"},
            {"E, MMMM dd, yyyy", "yMd"},
            {"E, MMMM d yyyy", "yMd"},
            {"EEE, MMMM d, yyyy", "yMd"},

    };
    private static final String[] CIRCA_PREFIXES = {
            "", "Circa", "c."
    };
    private static final String[] ERA_SUFFIXES = {
            "", "BC", "B.C.", "BCE", "B.C.E", "AD", "A.D.", "CE", "C.E.", "Common Era C.E."
    };
    @Getter(lazy=true) private static final String[][] formats = initFormats();

    private static String[][] initFormats () {
        List<String[]> allFormats = new ArrayList<>();
        for (String[] format : FORMAT_BASES) {
            for (String suffix : ERA_SUFFIXES) {
                if (suffix.length() > 0) suffix = " '" + suffix + "'";
                for (String prefix : CIRCA_PREFIXES) {
                    if (prefix.length() > 0) prefix = "'" + prefix + "' ";
                    allFormats.add(new String[]{prefix + format[0] + suffix, format[1]});
                    allFormats.add(new String[]{prefix + format[0] + ", " + suffix, format[1]});
                }
            }
        }
        return allFormats.toArray(new String[allFormats.size()][2]);
    }

    public static final String ERA_GROUP = "(?:\\s+(?:B\\.?C\\.?(?:E\\.?)?|C\\.?E\\.?|A\\.?D\\.?)?)?";

    public static final String MATCH_DAY  = "(\\d{1,2})";
    public static final String MATCH_YEAR = "(\\d{1,4})";

    public static final String MATCH_MONTH
            = "(January|Jan|February|Feb|March|Mar|April|Apr|May|June|Jun|July|Jul|August|Aug|September|Sep|October|Oct|November|Nov|December|Dec)";

    public static final String SPACE = "\\s+";
    public static final String ANY_SPACES = "\\s*";
    public static final String HYPHEN = "(?:[-–]+|spaced\\s*&?ndash|&?ndash)";

    public static final RangePattern[] RANGE_PATTERNS = {
            new RangePattern(MATCH_DAY + SPACE + MATCH_MONTH + SPACE + MATCH_YEAR + ANY_SPACES + HYPHEN + ANY_SPACES + MATCH_DAY + SPACE + MATCH_MONTH + SPACE + MATCH_YEAR,
                    "startDay", "startMonth", "startYear", "endDay", "endMonth", "endYear"),

            new RangePattern(MATCH_DAY + SPACE + MATCH_MONTH + ANY_SPACES + HYPHEN + ANY_SPACES + MATCH_DAY + SPACE + MATCH_MONTH + SPACE + MATCH_YEAR,
                    "startDay", "startMonth", "endDay", "endMonth", "startYear"),

            new RangePattern(MATCH_MONTH + SPACE + MATCH_DAY + ANY_SPACES + HYPHEN + ANY_SPACES + MATCH_MONTH + ANY_SPACES + MATCH_DAY + ",?" + SPACE + MATCH_YEAR,
                    "startMonth", "startDay", "endMonth", "endDay", "startYear"),

            new RangePattern(MATCH_MONTH + SPACE + MATCH_YEAR + ANY_SPACES + HYPHEN + ANY_SPACES + MATCH_MONTH + ANY_SPACES + SPACE + MATCH_YEAR,
                    "startMonth", "startYear", "endMonth", "endYear"),

            new RangePattern(MATCH_DAY + ANY_SPACES + HYPHEN + ANY_SPACES + MATCH_DAY + SPACE + MATCH_MONTH + ",?" + SPACE + MATCH_YEAR,
                    "startDay", "endDay", "startMonth", "startYear"),

            new RangePattern(MATCH_MONTH + SPACE + MATCH_DAY + ANY_SPACES + HYPHEN + ANY_SPACES + MATCH_DAY + ",?" + SPACE + MATCH_YEAR,
                    "startMonth", "startDay", "endDay", "startYear"),

            new RangePattern(MATCH_MONTH + ANY_SPACES + "or(?:\\s+(?:early|late))?" + ANY_SPACES + MATCH_MONTH + ",?" + ANY_SPACES + MATCH_YEAR,
                    "startMonth", null, "startYear"),

            new RangePattern(MATCH_YEAR + ANY_SPACES + "or(?:\\s+(?:early|late))?" + ANY_SPACES + MATCH_YEAR,
                    "startYear", null, null),

            new RangePattern(MATCH_YEAR + ANY_SPACES + HYPHEN + ANY_SPACES + MATCH_YEAR,
                    "startYear", "endYear"),

            new RangePattern("(?:.+?\\s+in\\s+)?" + MATCH_YEAR + ",?" + ANY_SPACES + "(?:as |under )?",
                    "startYear"),

            new RangePattern(MATCH_MONTH + ANY_SPACES + "/" + ANY_SPACES + MATCH_MONTH + ANY_SPACES +",?" + ANY_SPACES + MATCH_YEAR,
                    "startMonth", null, "startYear"),

            new RangePattern(OrdinalCentury.getMatchGroup() + ANY_SPACES + "century",
                    "startCentury"),
    };

    private static DateTimeFormatter[] DATE_FORMATTERS;
    static {
        final String[][] ALL_FORMATS = getFormats();
        DATE_FORMATTERS = new DateTimeFormatter[ALL_FORMATS.length];
        for (int i = 0; i< ALL_FORMATS.length; i++) {
            try {
                DATE_FORMATTERS[i] = DateTimeFormat.forPattern(ALL_FORMATS[i][0]);
            } catch (Exception e) {
                die("Error in format: "+ALL_FORMATS[i][0]);
            }
        }

        for (RangePattern rangePattern : RANGE_PATTERNS) {
            rangePattern.setEras(ERA_SUFFIXES);
        }
    }

    public static TimeRange parse(String input) {

        if (empty(input)) return null;

        String date = scrub(input);
        final boolean bce = isBce(date);

        for (RangePattern rangePattern : RANGE_PATTERNS) {
            final Matcher matcher = rangePattern.matches(date);
            if (matcher == null) continue;
            return fromMatcher(bce, rangePattern, matcher);
        }

        int orIndex = date.indexOf(" or ");
        if (orIndex != -1) date = date.substring(0, orIndex);

        final TimePoint start = new TimePoint();
        final String[][] ALL_FORMATS = getFormats();
        for (int i=0; i<DATE_FORMATTERS.length; i++) {
            final DateTimeFormatter formatter = DATE_FORMATTERS[i];
            try {
                DateTime dateTime = formatter.parseDateTime(date);
                final String fieldMask = ALL_FORMATS[i][1];
                for (char c : fieldMask.toCharArray()) {
                    switch (c) {
                        case 'y':
                            start.setYear(dateTime.getYear());
                            if (bce) start.invertYear();
                            break;
                        case 'M': start.setMonth((byte) dateTime.getMonthOfYear()); break;
                        case 'd': start.setDay((byte) dateTime.getDayOfMonth()); break;
                        case 'h': start.setHour((byte) dateTime.getHourOfDay()); break;
                        case 'm': start.setMinute((byte) dateTime.getMinuteOfHour()); break;
                        case 's': start.setSecond((byte) dateTime.getSecondOfMinute()); break;
                        default: die("invalid field mask value ("+fieldMask+"): "+c);
                    }
                }
                return new TimeRange(start);

            } catch (Exception ignored) {
                // noop
            }
        }

        // try range patterns with "find" instead of match
        for (RangePattern rangePattern : RANGE_PATTERNS) {
            final Matcher matcher = rangePattern.find(date);
            if (matcher == null) continue;
            return fromMatcher(bce, rangePattern, matcher);
        }

        return die("parse: no formats matched: "+input);
    }

    public static TimeRange fromMatcher(boolean bce, RangePattern rangePattern, Matcher matcher) {
        final TimePoint start = new TimePoint();
        final TimePoint end = new TimePoint();
        final Set<String> wroteToStart = new HashSet<>();
        final Set<String> wroteToEnd = new HashSet<>();
        for (int i=0; i<rangePattern.getNumFields(); i++) {
            String field = rangePattern.getField(i);
            final int groupIndex = i+1;
            if (field == null) continue;
            TimePoint target;
            if (field.startsWith("end")) {
                target = end;
                field = uncapitalize(field.substring("end".length()));
                wroteToEnd.add(field);

            } else {
                target = start;
                if (field.startsWith("start")) field = uncapitalize(field.substring("start".length()));
                wroteToStart.add(field);
            }

            if (field.equals("century")) {
                set(target, field, matcher.group(groupIndex));
                if (bce) target.invertYear();

            } else if (field.equals("year")) {
                set(target, field, Long.parseLong(matcher.group(groupIndex)));
                if (bce) target.invertYear();

            } else if (field.equals("month")) {
                final Byte month = parseMonth(matcher.group(groupIndex));
                if (month != null) set(target, field, month);

            } else {
                set(target, field, Byte.parseByte(matcher.group(groupIndex)));
            }
        }
        // if we wrote fields to start, but not to end, copy their values from start -> end
        for (String field : wroteToStart) {
            if (!wroteToEnd.contains(field) || get(end, field) == null) {
                if (field.equals("century")) field = "year";
                set(end, field, get(start, field));
            }
        }

        // If the endYear is 2 digits, prepend the first 2 digits of the start year
        if (String.valueOf(Math.abs(end.getYear())).length() == 2) {
            end.setYear((100*(start.getYear() / 100)) + end.getYear());
        }

        start.initInstant();
        end.initInstant();
        return new TimeRange(start, end);
    }

    private static Byte parseMonth(String month) {
        switch (month.trim().toLowerCase()) {
            case "january": case "jan": return 1;
            case "february": case "feb": return 2;
            case "march": case "mar": return 3;
            case "april": case "apr": return 4;
            case "may": return 5;
            case "june": case "jun": return 6;
            case "july": case "jul": return 7;
            case "august": case "aug": return 8;
            case "september": case "sep": return 9;
            case "october": case "oct": return 10;
            case "november": case "nov": return 11;
            case "december": case "dec": return 12;
            default: return null;
        }
    }

    public static boolean isBce(String date) {
        date = date.replace(".", "").trim();
        return date.endsWith("BC") || date.endsWith("BCE") || date.endsWith("B.C.") || date.endsWith("B.C.E.");
    }

    private static String scrub(String date) {
        date = date.trim();
        if (date.startsWith("\"") || date.startsWith("'")) date = date.substring(1);
        if (date.endsWith("\"") || date.endsWith("'")) date = date.substring(0, date.length()-1);
        int pos = date.indexOf("<");
        if (pos != -1) date = date.substring(0, pos).trim();
        pos = date.indexOf("&lt;");
        if (pos != -1) date = date.substring(0, pos).trim();
        pos = date.indexOf("{{");
        if (pos != -1) date = date.substring(0, pos).trim();

        pos = date.indexOf("(or");
        if (pos != -1) {
            int endPos = date.indexOf(")", pos);
            date = date.substring(0, pos).trim() + " " + date.substring(endPos+1);
        }

        pos = date.indexOf("(");
        int endPos = date.indexOf(")");
        if (pos != -1) {
            if (endPos != -1 && endPos != date.length()-1) {
                date = (date.substring(0, pos) + date.substring(endPos+1)).trim();
            } else {
                date = date.substring(0, pos).trim();
            }
        }

        date = date.replace("&amp;nbsp;", "")
                .replace("&amp;", "").replace("amp;", "")
                .replace("&nbsp;", "").replace("nbsp;", "")
                // sometimes ANTLR can't parse hyphen character and instead produces one of these
                .replace("\ufffd", "-");

        return date.trim();
    }

}
