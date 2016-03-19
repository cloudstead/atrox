package histori.dao;

import histori.ApiConstants;
import histori.dao.shard.TagShardDAO;
import histori.model.Nexus;
import histori.model.NexusTag;
import histori.model.Tag;
import histori.model.support.AutocompleteSuggestions;
import lombok.AllArgsConstructor;
import org.cobbzilla.util.cache.AutoRefreshingReference;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;

@Repository public class TagDAO extends ShardedEntityDAO<Tag, TagShardDAO> {

    private static final long AUTOCOMPLETE_CACHE_TIMEOUT = TimeUnit.HOURS.toMillis(2);

    @Autowired private DatabaseConfiguration database;
    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("tag"); }

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

    public List<Tag> findByCanonicalNames(String[] names) {
        if (empty(names)) return new ArrayList<>();
        final String[] canonical = new String[names.length];
        for (int i=0; i<names.length; i++) canonical[i] = canonicalize(names[i]);
        return findByFieldIn("canonicalName", canonical);
    }

    // findByCanonicalName needs to be lightning-fast
    private static final long TAG_CACHE_REFRESH = TimeUnit.MINUTES.toMillis(10);
    private final AtomicLong lastCacheFill = new AtomicLong(0);
    private final Map<String, Tag> tagCache = new ConcurrentHashMap<>();

    public Tag findByCanonicalName(String canonicalName) {
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

    private final Map<String, AutoRefreshingReference<AutocompleteSuggestions>> autocompleteCache = new ConcurrentHashMap<>();

    public AutocompleteSuggestions findByCanonicalNameStartsWith(String nameFragment) {
        return findByCanonicalNameStartsWith(nameFragment, null);
    }

    public AutocompleteSuggestions findByCanonicalNameStartsWith(final String nameFragment, final String matchType) {
        final String cacheKey = sha256_hex(canonicalize(matchType)+":"+canonicalize(nameFragment));
        final AutoRefreshingReference<AutocompleteSuggestions> cached = autocompleteCache.get(cacheKey);
        if (cached == null) autocompleteCache.put(cacheKey, new AutocompleteSuggestionsAutoRefreshingReference(matchType, nameFragment));
        return autocompleteCache.get(cacheKey).get();
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

    @AllArgsConstructor
    private class AutocompleteSuggestionsAutoRefreshingReference extends AutoRefreshingReference<AutocompleteSuggestions> {
        private final String matchType;
        private final String nameFragment;

        @Override public AutocompleteSuggestions refresh() {
            AutocompleteSuggestions suggestions = new AutocompleteSuggestions();

            final List<Tag> tags;
            if (matchType == null) {
                tags = findByFieldLike("canonicalName", canonicalize(nameFragment)+"%");
            } else if (matchType.equals(ApiConstants.MATCH_NULL_TYPE)) {
                tags = findByFieldNullAndFieldLike("tagType", "canonicalName", canonicalize(nameFragment) + "%");
            } else {
                tags = findByFieldEqualAndFieldLike("tagType", canonicalize(matchType), "canonicalName", canonicalize(nameFragment) + "%");
            }
            for (Tag tag : tags) suggestions.add(tag.getName(), tag.getTagType());
            return suggestions;
        }

        @Override public long getTimeout() { return AUTOCOMPLETE_CACHE_TIMEOUT; }
    }
}
