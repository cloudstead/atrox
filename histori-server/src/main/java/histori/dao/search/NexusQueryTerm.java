package histori.dao.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Transient;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.trimQuotes;

@Slf4j @NoArgsConstructor @AllArgsConstructor @Accessors(chain=true) @ToString(of={"term","fieldType","matchType"})
public class NexusQueryTerm implements Comparable<NexusQueryTerm> {

    public static final String FIELD_NAME = "name";
    public static final String FIELD_NEXUS_TYPE = "nexusType";
    public static final String FIELD_MARKDOWN = "markdown";
    public static final String FIELD_TAG_NAME = "tagName";
    public static final String FIELD_TAG_TYPE = "tagType";
    public static final String FIELD_DECORATOR_NAME = "decoratorName";
    public static final String FIELD_DECORATOR_VALUE = "decoratorValue";

    @Override public int compareTo(NexusQueryTerm o) {
        return o == null ? Integer.MAX_VALUE : o.toString().compareTo(toString());
    }

    @AllArgsConstructor public enum MatchType {

        fuzzy ("f"), exact ("e"), regex ("r");

        @Getter private String quickName;
        @JsonCreator public static MatchType create (String val) {
            if (empty(val)) return fuzzy;
            val = val.toLowerCase();
            for (MatchType t : values()) if (t.name().equals(val) || t.quickName.equals(val)) return t;
            log.warn("create("+val+"): unrecognized MatchType");
            return fuzzy;
        }
    }

    @AllArgsConstructor public enum FieldType {

        any ("*"), any_including_markdown ("@"), name ("n"), nexus_type ("N"), markdown ("m"),
        tags ("T"), tag_name ("t"), tag_type ("y"), decorator_name ("D"), decorator_value ("d");

        @Getter private String quickName;
        @JsonCreator public static FieldType create (String val) {
            if (empty(val)) return any;
            val = val.toLowerCase().replace("-", "_");
            for (FieldType t : values()) if (t.name().equals(val) || t.quickName.equals(val)) return t;
            log.warn("create("+val+"): unrecognized FieldType");
            return any;
        }
    }

    @Getter @Setter private String term;
    @Getter @Setter private FieldType fieldType;
    @Getter @Setter private MatchType matchType;

    @Getter(lazy=true, value=AccessLevel.PROTECTED) private final Pattern pattern = initPattern();
    private Pattern initPattern() { return Pattern.compile(term); }

    @JsonIgnore @Transient public boolean isFuzzy () { return matchType == null || matchType == MatchType.fuzzy; }
    @JsonIgnore @Transient public boolean isExact () { return matchType == MatchType.exact; }
    @JsonIgnore @Transient public boolean isRegex () { return matchType == MatchType.regex; }

    @JsonIgnore @Transient public boolean isAny           () { return fieldType == null || fieldType == FieldType.any; }
    @JsonIgnore @Transient public boolean isAnyIncludingMarkdown () { return fieldType == FieldType.any_including_markdown; }
    @JsonIgnore @Transient public boolean isName          () { return isAny() || fieldType == FieldType.name; }
    @JsonIgnore @Transient public boolean isNexusType     () { return isAny() || fieldType == FieldType.nexus_type; }
    @JsonIgnore @Transient public boolean isMarkdown      () { return isAnyIncludingMarkdown() || fieldType == FieldType.markdown; }
    @JsonIgnore @Transient public boolean isTags          () { return isAny() || fieldType == FieldType.tags; }
    @JsonIgnore @Transient public boolean isTagName       () { return isAny() || fieldType == FieldType.tag_name; }
    @JsonIgnore @Transient public boolean isTagType       () { return isAny() || fieldType == FieldType.tag_type; }
    @JsonIgnore @Transient public boolean isDecoratorName () { return isAny() || fieldType == FieldType.decorator_name; }
    @JsonIgnore @Transient public boolean isDecoratorValue() { return isAny() || fieldType == FieldType.decorator_value; }

    public boolean matchesField(String field) {
        if (isAnyIncludingMarkdown()) return true;
        if (isAny()) return !field.equals("markdown");
        switch (field) {
            case FIELD_NAME: return isName();
            case FIELD_NEXUS_TYPE: return isNexusType();
            case FIELD_MARKDOWN: return isMarkdown();
            case FIELD_TAG_NAME: return isTags() || isTagName();
            case FIELD_TAG_TYPE: return isTags() || isTagType();
            case FIELD_DECORATOR_NAME: return isTags() || isDecoratorName();
            case FIELD_DECORATOR_VALUE: return isTags() || isDecoratorValue();
            default:
                log.warn("unrecognized field: "+field);
                return false;
        }
    }

    public boolean matchesTerm(String value) {
        if (isFuzzy()) return term.toLowerCase().contains(value.toLowerCase().trim());
        if (isExact()) return term.toLowerCase().trim().equals(value.toLowerCase().trim());
        if (isRegex()) return getPattern().matcher(value).find();
        log.warn("matchesTerm("+value+"): no matcher applied (term="+toString()+")");
        return false;
    }

    public static final String MATCH_TYPE_REGEX = "[efr]|exact|fuzzy|regex";
    public static final String FIELD_TYPE_REGEX = "[nNmTtyDd]|name|nexus[-_ ]type|markdown|tags|tag[-_ ]name|tag[-_ ]type|decorator[-_ ]name|decorator[-_ ]value";
    public static final String TERM_REGEX = "^((?<match>"+MATCH_TYPE_REGEX+"):)?((?<field>"+FIELD_TYPE_REGEX+"):)?(?<query>.+)$";
    public static final Pattern TERM_PATTERN = Pattern.compile(TERM_REGEX);

    public NexusQueryTerm (String term) {
        this.term = term;
        this.fieldType = FieldType.any;
        this.matchType = MatchType.fuzzy;
    }

    public static NexusQueryTerm create (String input) {
        input = input.trim();
        if (input.isEmpty()) {
            log.warn("NexusQueryTerm.create: empty input");
            return null;
        }

        final Matcher matcher = TERM_PATTERN.matcher(input);
        if (!matcher.find()) {
            log.warn("NexusQueryTerm.create: no term matched");
            return new NexusQueryTerm().setTerm(input);
        }

        return new NexusQueryTerm()
                .setMatchType(MatchType.create(matcher.group("match")))
                .setFieldType(FieldType.create(matcher.group("field")))
                .setTerm(trimQuotes(matcher.group("query")));
    }

}
