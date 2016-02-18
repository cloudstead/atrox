package histori.dao;

import histori.model.Tag;
import histori.model.support.AutocompleteSuggestions;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.wizard.model.ResultPage;
import org.springframework.stereotype.Repository;

import java.util.List;

import static histori.ApiConstants.MATCH_NULL_TYPE;
import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Repository public class TagDAO extends CanonicalEntityDAO<Tag> {

    public static final String AUTOCOMPLETE_SQL
            = "from Tag t " +
            "where t.canonicalName like :nameFragment ";
    public static final String AUTOCOMPLETE_INCLUDE =
            "and t.tagType = :tagType ";
    public static final String AUTOCOMPLETE_NULLTYPE =
            "and t.tagType is null ";
    public static final String AUTOCOMPLETE_ORDER =
            "order by length(t.canonicalName) desc";

    public AutocompleteSuggestions findByCanonicalNameStartsWith(String nameFragment) {
        return findByCanonicalNameStartsWith(nameFragment, null);
    }

    public AutocompleteSuggestions findByCanonicalNameStartsWith(String nameFragment, String matchType) {

        // todo -- we really need to cache this stuff...
        final AutocompleteSuggestions suggestions = new AutocompleteSuggestions();

        Object[] values = {nameFragment+"%"};
        String[] params = new String[]{"nameFragment"};

        String sql = AUTOCOMPLETE_SQL;
        if (!empty(matchType)) {
            if (matchType.equals(MATCH_NULL_TYPE)) {
                sql += AUTOCOMPLETE_NULLTYPE;
            } else {
                sql += AUTOCOMPLETE_INCLUDE;
                values = ArrayUtil.append(values, canonicalize(matchType));
                params = ArrayUtil.append(params, "tagType");
            }
        }
        sql += AUTOCOMPLETE_ORDER;

        final List<Tag> tags = query(sql, ResultPage.DEFAULT_PAGE, params, values);
        for (Tag tag : tags) suggestions.add(tag.getName(), tag.getTagType());
        return suggestions;
    }

}
