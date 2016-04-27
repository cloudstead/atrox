package histori.dao.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.CanonicalEntity;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Transient;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
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
    @Getter @Setter private FieldType fieldType = FieldType.any;
    @Getter @Setter private MatchType matchType = MatchType.fuzzy;

    @Getter(lazy=true, value=AccessLevel.PROTECTED) private final Pattern pattern = initPattern();
    private Pattern initPattern() { return Pattern.compile(term); }

    public String sqlClause() { return sqlClause(fieldType, matchType); }

    public void sqlArgs(List<Object> args) { sqlArgs(fieldType, term, args); }

    private String sqlArg(String term) { return sqlArg(term, false); }

    private String sqlArg(String term, boolean canonicalize) {
        switch (matchType) {
            case exact:          return canonicalize ? canonicalize(term) : term;
            case regex:          return term;
            case fuzzy: default:
                if (canonicalize) {
                    term = canonicalize(term).replace(CanonicalEntity.WORD_SEP, "%");
                } else {
                    term = term.replaceAll("\\s+", " ").replace(" ", "%");
                }
                return "%" + term + "%";
        }
    }

    public void sqlArgs(FieldType fieldType, String arg, List<Object> args) {
        switch (fieldType) {
            case any:
                sqlArgs(FieldType.name, arg, args);
                sqlArgs(FieldType.nexus_type, arg, args);
                sqlArgs(FieldType.tags, arg, args);
                break;

            case any_including_markdown:
                sqlArgs(FieldType.any, arg, args);
                sqlArgs(FieldType.markdown, arg, args);
                break;

            case tags:
                sqlArgs(FieldType.tag_name, arg, args);
                sqlArgs(FieldType.decorator_value, arg, args);
                break;

            case name:
            case tag_name:
                args.add(sqlArg(arg, true));
                break;

            case nexus_type:
            case tag_type:
            case decorator_name:
            case decorator_value:
            case markdown:
                args.add(sqlArg(arg));
                break;

            default: die("sqlArgs: invalid fieldType: "+fieldType);
        }
    }

    private static final String FIND_TAG
            = "(SELECT count(*) FROM "
            + "   (SELECT x::jsonb ->> 'canonicalName' canonicalName, x::jsonb ->> 'tagType' tagType "
            + "    FROM jsonb_array_elements(n.tags->'tags') as x) y "
            + "    WHERE @@WHERE@@"
            + ") > 0";
    private static final String FIND_DECORATOR
            = "(select count(*) FROM jsonb_to_recordset($tags -> 'tags') as x(values text)"
            + "  WHERE x.values is not null"
            + "  AND (SELECT count(*) FROM jsonb_array_elements(x.values::jsonb) y WHERE @@WHERE@@) > 0"
            + ") > 0";
    public static String sqlClause(FieldType fieldType, MatchType matchType) {
        switch (fieldType) {
            case any: default:
                return "(" + sqlClause(FieldType.name, matchType)
                  + " OR " + sqlClause(FieldType.nexus_type, matchType)
                  + " OR " + sqlClause(FieldType.tags, matchType) + ")";

            case any_including_markdown:
                return "(" + sqlClause(FieldType.any, matchType)
                  + " OR " + sqlClause(FieldType.markdown, matchType) + ")";

            case tags:
                return "(" + sqlClause(FieldType.tag_name, matchType)
                  + " OR " + sqlClause(FieldType.decorator_value, matchType) + ")";

            case name:       return sqlComparison("$canonical_name", matchType);
            case nexus_type: return sqlComparison("$nexus_type", matchType);
            case markdown:   return sqlComparison("$markdown", matchType);

            case tag_name:   return FIND_TAG.replace("@@WHERE@@", sqlComparison("y.canonicalName", matchType));
            case tag_type:   return FIND_TAG.replace("@@WHERE@@", sqlComparison("y.tagType", matchType));

            case decorator_name:
                return FIND_DECORATOR.replace("@@WHERE@@", sqlComparison("(y::jsonb ->> 'field')", matchType));
            case decorator_value:
                return FIND_DECORATOR.replace("@@WHERE@@", sqlComparison("(y::jsonb ->> 'value')", matchType));
        }
    }

    private static String sqlComparison(String name, MatchType matchType) {
        switch (matchType) {
            case fuzzy: default: return "( " + name + " ilike ? )";
            case exact: return "( " + name + " = ? )";
            case regex: return "( " + name + " ~* ? )";
        }
    }

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
    public static final String FIELD_TYPE_REGEX = "[nNmTtyDd\\*@]|any[-_ ]including[-_ ]markdown|any|name|nexus[-_ ]type|markdown|tags|tag[-_ ]name|tag[-_ ]type|decorator[-_ ]name|decorator[-_ ]value";

    public static final String QUALIFIER_REGEX = "^((?<match>"+MATCH_TYPE_REGEX+"):)?((?<field>"+FIELD_TYPE_REGEX+"):)?";
    public static final Pattern BARE_QUALIFIER_PATTERN = Pattern.compile(QUALIFIER_REGEX+"$");

    public static final String TERM_REGEX = QUALIFIER_REGEX + "(?<query>.+)$";
    public static final Pattern TERM_PATTERN = Pattern.compile(TERM_REGEX);

    public static boolean isBareQualifier(String val) {
        final Matcher matcher = BARE_QUALIFIER_PATTERN.matcher(val);
        return matcher.find() && (matcher.group("match") != null || matcher.group("field") != null);
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
