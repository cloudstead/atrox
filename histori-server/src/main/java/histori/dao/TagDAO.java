package histori.dao;

import histori.model.Nexus;
import histori.model.NexusTag;
import histori.model.Tag;
import histori.model.support.AutocompleteSuggestions;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.wizard.model.ResultPage;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static histori.ApiConstants.MATCH_NULL_TYPE;
import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;

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

    @Override public Tag postCreate(Tag tag, Object context) {
        tagCache.put(tag.getCanonicalName(), tag);
        return super.postCreate(tag, context);
    }

    @Override public Tag postUpdate(@Valid Tag tag, Object context) {
        tagCache.put(tag.getCanonicalName(), tag);
        return super.postUpdate(tag, context);
    }

    @Override public Tag create(@Valid Tag entity) {
        final Tag found = tagCache.get(entity.getCanonicalName());
        return found != null ? found : super.create(entity);
    }

    // findByCanonicalName needs to be lightning-fast
    private static final long TAG_CACHE_REFRESH = TimeUnit.MINUTES.toMillis(10);
    private final AtomicLong lastCacheFill = new AtomicLong(0);
    private final Map<String, Tag> tagCache = new ConcurrentHashMap<>();
    @Override public Tag findByCanonicalName(String canonicalName) {
        if (now() - lastCacheFill.get() > TAG_CACHE_REFRESH) {
            synchronized (lastCacheFill) {
                if (now() - lastCacheFill.get() > TAG_CACHE_REFRESH) {
                    for (Tag tag : findAll()) {
                        tagCache.put(tag.getCanonicalName(), tag);
                    }
                    lastCacheFill.set(now());
                }
            }
        }
        return tagCache.get(canonicalName);
    }

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

    public void updateTags(Nexus nexus) {
        if (!nexus.hasTags()) return;
        boolean added = false;
        for (NexusTag nexusTag : nexus.getTags()) {
            Tag tag = findByCanonicalName(nexusTag.getCanonicalName());
            if (tag == null) {
                create(new Tag(nexusTag.getTagName(), nexusTag.getTagType()));
                added = true;
            }
            if (nexusTag.hasSchemaValues()) {
                for (Map.Entry<String, TreeSet<String>> entry : nexusTag.getSchemaValueMap().allEntrySets()) {
                    for (String value : entry.getValue()) {
                        tag = findByCanonicalName(canonicalize(value));
                        if (tag == null) {
                            create(new Tag(value, entry.getKey()));
                            added = true;
                        }
                    }
                }
            }
        }
        if (added) lastCacheFill.set(0);
    }
}
