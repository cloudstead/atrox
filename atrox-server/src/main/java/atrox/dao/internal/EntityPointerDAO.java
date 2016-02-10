package atrox.dao.internal;

import atrox.model.internal.EntityPointer;
import atrox.model.support.AutocompleteSuggestions;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.model.ResultPage;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Repository public class EntityPointerDAO extends AbstractCRUDDAO<EntityPointer> {

    public EntityPointer findByCanonicalName(String canonicalName) {
        return empty(canonicalName) ? null : findByUniqueField("canonicalName", canonicalName);
    }

    public static final String AUTOCOMPLETE_SQL
            = "from EntityPointer p " +
            "where p.canonicalName like :nameFragment ";
    public static final String AUTOCOMPLETE_INCLUDE =
            "and p.entityType in :includeTypes ";
    public static final String AUTOCOMPLETE_EXCLUDE =
            "and not p.entityType in :excludeTypes ";
    public static final String AUTOCOMPLETE_ORDER =
            "order by length(p.canonicalName) desc";

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

        final List<EntityPointer> pointers = query(sql, ResultPage.DEFAULT_PAGE, params, values, listParams);
        for (EntityPointer p : pointers) suggestions.add(p.getName(), p.getEntityType());
        return suggestions;
    }
}
