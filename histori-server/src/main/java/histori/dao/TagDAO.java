package histori.dao;

import histori.ApiConstants;
import histori.dao.shard.TagShardDAO;
import histori.model.Nexus;
import histori.model.NexusTag;
import histori.model.Tag;
import histori.model.support.AutocompleteSuggestions;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.cache.AutoRefreshingReference;
import org.cobbzilla.util.cache.BackgroundRefreshingReference;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;
import static org.cobbzilla.util.string.StringUtil.isNumber;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@Repository @Slf4j
public class TagDAO extends ShardedEntityDAO<Tag, TagShardDAO> {

    private static final long AUTOCOMPLETE_CACHE_TIMEOUT = TimeUnit.HOURS.toMillis(2);

    @Autowired private DatabaseConfiguration database;
    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("tag"); }

    @Override public Object preCreate(@Valid Tag entity) {
        if (isNumber(entity.getName())) throw invalidEx("err.tagName.isNumber");
        return super.preCreate(entity);
    }

    @Override public Tag postCreate(Tag tag, Object context) {
        updateCache(tag);
        return super.postCreate(tag, context);
    }

    @Override public Tag postUpdate(@Valid Tag tag, Object context) {
        updateCache(tag);
        return super.postUpdate(tag, context);
    }

    @Override public Tag create(@Valid Tag tag) {
        final Map<String, Tag> cache = tagCache.get();
        Tag found = null;
        if (cache != null) found = cache.get(tag.getCanonicalName());
        if (found == null) found = findByCanonicalNameNoCache(tag.getCanonicalName());
        return found != null ? found : super.create(tag);
    }

    public List<Tag> findByCanonicalNames(String[] names) {
        if (empty(names)) return new ArrayList<>();
        final String[] canonical = new String[names.length];
        for (int i=0; i<names.length; i++) canonical[i] = canonicalize(names[i]);
        return findByFieldIn("canonicalName", canonical);
    }

    // findByCanonicalName needs to be lightning-fast
    private static final long TAG_CACHE_REFRESH = TimeUnit.MINUTES.toMillis(10);

    private final BackgroundRefreshingReference<Map<String, Tag>> tagCache = new BackgroundRefreshingReference<Map<String, Tag>>() {
        @Override public boolean initialize() { return false; }

        @Override public Map<String, Tag> refresh() {
            final Map<String, Tag> cache = new ConcurrentHashMap<>();
            for (Tag tag : findAll()) {
                cache.put(tag.getCanonicalName(), tag);
            }
            return cache;
        }

        @Override public long getTimeout() { return TAG_CACHE_REFRESH; }
    };

    protected void updateCache(Tag tag) {
        final Map<String, Tag> cache = tagCache.get();
        if (cache != null) cache.put(tag.getCanonicalName(), tag);
    }

    public void warmCache () { tagCache.update(); }

    public Tag findByCanonicalName(String canonicalName) {
        final Map<String, Tag> cache = tagCache.get();
        if (cache != null) return cache.get(canonicalName);
        log.info("findByCanonicalName: cache not initialized, doing real-time lookup of "+canonicalName);
        return findByCanonicalNameNoCache(canonicalName);
    }

    public Tag findByCanonicalNameNoCache(String canonicalName) { return super.findByName(canonicalName); }

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
            if (empty(nexusTag.getTagName())) {
                log.info("skipping empty tag: "+nexusTag);
                continue;
            }
            if (isNumber(nexusTag.getTagName())) {
                log.info("skipping numeric tag: "+nexusTag);
                continue;
            }
            Tag tag = findByCanonicalName(nexusTag.getCanonicalName());
            if (tag == null) {
                create(new Tag(nexusTag.getTagName(), nexusTag.getTagType()));
                added = true;
            }
            if (nexusTag.hasSchemaValues()) {
                for (Map.Entry<String, TreeSet<String>> entry : nexusTag.getSchemaValueMap().allEntrySets()) {
                    for (String value : entry.getValue()) {
                        if (isNumber(value)) {
                            log.debug("skipping numeric tag: "+value);
                            continue;
                        }
                        tag = findByCanonicalName(canonicalize(value));
                        if (tag == null) {
                            create(new Tag(value, entry.getKey()));
                            added = true;
                        }
                    }
                }
            }
        }
        if (added) tagCache.set(null);
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
            } else {
                final String tagType = matchType.equals(ApiConstants.MATCH_NULL_TYPE) ? null : canonicalize(matchType);
                tags = findByFieldEqualAndFieldLike("tagType", tagType, "canonicalName", canonicalize(nameFragment) + "%");
            }
            for (Tag tag : tags) suggestions.add(tag.getName(), tag.getTagType());
            return suggestions;
        }

        @Override public long getTimeout() { return AUTOCOMPLETE_CACHE_TIMEOUT; }
    }
}
