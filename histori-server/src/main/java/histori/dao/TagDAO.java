package histori.dao;

import histori.model.Tag;
import histori.model.support.AutocompleteSuggestions;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.wizard.model.ResultPage;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Repository public class TagDAO extends CanonicalEntityDAO<Tag> {

    public static final String AUTOCOMPLETE_SQL
            = "from Tag t " +
            "where t.canonicalName like :nameFragment ";
    public static final String AUTOCOMPLETE_INCLUDE =
            "and t.tagType in :includeTypes ";
    public static final String AUTOCOMPLETE_EXCLUDE =
            "and not t.tagType in :excludeTypes ";
    public static final String AUTOCOMPLETE_ORDER =
            "order by length(t.canonicalName) desc";

    public AutocompleteSuggestions findByCanonicalNameStartsWith(String nameFragment) {
        return findByCanonicalNameStartsWith(nameFragment, null, null);
    }

    public AutocompleteSuggestions findByCanonicalNameStartsWith(String nameFragment, List<String> includeTypes) {
        return findByCanonicalNameStartsWith(nameFragment, includeTypes, null);
    }

    public AutocompleteSuggestions findByCanonicalNameStartsWith(String nameFragment, List<String> includeTypes, List<String> excludeTypes) {

        // todo -- we really need to cache this stuff...
        final AutocompleteSuggestions suggestions = new AutocompleteSuggestions();

        Object[] values = {nameFragment+"%"};
        String[] params = new String[]{"nameFragment"};
        String[] listParams = new String[0];

        String sql = AUTOCOMPLETE_SQL;
        if (!empty(includeTypes)) {
            sql += AUTOCOMPLETE_INCLUDE;
            values = ArrayUtil.append(values, includeTypes);
            listParams = ArrayUtil.append(listParams, "includeTypes");
        }
        if (!empty(excludeTypes)) {
            sql += AUTOCOMPLETE_EXCLUDE;
            values = ArrayUtil.append(values, excludeTypes);
            listParams = ArrayUtil.append(listParams, "excludeTypes");
        }
        params = ArrayUtil.concat(params, listParams);
        sql += AUTOCOMPLETE_ORDER;

        final List<Tag> tags = query(sql, ResultPage.DEFAULT_PAGE, params, values, listParams);
        for (Tag tag : tags) suggestions.add(tag.getName(), tag.getTagType());
        return suggestions;
    }

}
