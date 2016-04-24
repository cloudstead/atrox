package histori.dao.search;

import histori.dao.TagDAO;
import histori.dao.TagTypeDAO;
import histori.model.*;
import histori.model.base.NexusTags;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.dao.EntityFilter;

import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static histori.dao.search.NexusQueryTerm.*;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;

@Slf4j
public class NexusEntityFilter implements EntityFilter<NexusView> {

    private static final long FILTER_CACHE_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(1);

    @Getter private final RedisService filterCache;
    public boolean hasFilterCache () { return filterCache != null; }

    @Getter private final TagDAO tagDAO;
    @Getter private final TagTypeDAO tagTypeDAO;
    private final String queryHash;
    private final NexusQueryTerms terms;

    public NexusEntityFilter(String query, RedisService filterCache, TagDAO tagDAO, TagTypeDAO tagTypeDAO) {

        this.filterCache = filterCache;
        this.tagDAO = tagDAO;
        this.tagTypeDAO = tagTypeDAO;

        // by sorting terms alphabetically, and trimming whitespace, we prevent duplicate cache entries
        // for searches that are essentially the same
        terms = new NexusQueryTerms(query);
        queryHash = sha256_hex(StringUtil.toString(terms, " "));
    }

    @Override public boolean isAcceptable(NexusView nexus) {

        if (nexus instanceof SuperNexus) {
            // SuperNexus matches everything -- we only apply the EntityFilter to the top-matched Nexus within the SuperNexus
            return true;
        }
        if (empty(terms)) return false; // empty query matches nothing (note: used to match everything)

        if (terms.size() == 1 && terms.first().equals("*")) return true; // now a single '*' matches everything

        final String cacheKey = nexus.getUuid() + ":query:" + queryHash;
        String cached = getCached(cacheKey);

        if (!empty(cached)) return Boolean.valueOf(cached);

        boolean match = isMatch(nexus);

        setCacheKey(cacheKey, match);

        return match;
    }

    protected boolean isMatch(NexusView nexus) {

        // must match on all search terms
        for (NexusQueryTerm term : terms) {
            final String cacheKey = nexus.getUuid() + ":term:" + sha256_hex(term.toString());
            String cached = getCached(cacheKey);

            if (!empty(cached)) {
                boolean match = Boolean.valueOf(cached);
                setCacheKey(cacheKey, match); // update access-time, keep warm in cache
                return match;
            }

            boolean match = matchTerm(nexus, term);
            setCacheKey(cacheKey, match);

            if (!match) return false;
        }
        return true;
    }

    private String getCached(String cacheKey) {
        if (!hasFilterCache()) return null;
        try {
            return getFilterCache().get(cacheKey);
        } catch (Exception e) {
            log.error("Error reading from NexusEntityFilter query cache: " + e);
            return null;
        }
    }

    private void setCacheKey(String cacheKey, Boolean match) {
        if (hasFilterCache()) {
            try {
                getFilterCache().set(cacheKey, match.toString(), "EX", FILTER_CACHE_TIMEOUT_SECONDS);
            } catch (Exception e) {
                log.error("Error writing to NexusEntityFilter query-term cache: " + e);
            }
        }
    }

    protected boolean matchTerm(NexusView nexus, NexusQueryTerm term) {

        // name match is fuzzy
        if (nexus.getCanonicalName().equals("battle-of-waterloo")) {
            log.info("this should match");
        }
        if (matchField(NexusQueryTerm.FIELD_NAME, nexus.getName(), term)) return true;

        // type match must be exact (ignoring case)
        if (nexus.hasNexusType()) {
            final Tag tag = tagDAO.findByCanonicalName(nexus.getNexusType());
            if (tag != null && matchField(FIELD_NEXUS_TYPE, tag.getName(), term)) return true;
        }

        if (nexus.hasMarkdown() && matchField(FIELD_MARKDOWN, nexus.getMarkdown(), term)) return true;
        if (!nexus.hasTags()) return false;

        // check nexusTags
        final NexusTags nexusTags = nexus.getTags();
        for (NexusTag nexusTag : nexusTags) {

            // tag name match is fuzzy
            final Tag tag = tagDAO.findByCanonicalName(nexusTag.getCanonicalName());
            if (tag != null && matchField(FIELD_TAG_NAME, tag.getName(), term)) return true;

            // type match must be exact (ignoring case)
            final TagType tagType = tagTypeDAO.findByCanonicalName(nexusTag.getTagType());
            if (tagType != null && matchField(FIELD_TAG_TYPE, tagType.getName(), term)) return true;

            // all schema matches, both keys and (possibly multiple) values, are fuzzy matches
            if (nexusTag.hasSchemaValues()) {
                for (Map.Entry<String, TreeSet<String>> schema : nexusTag.getSchemaValueMap().allEntrySets()) {
                    if (matchField(FIELD_DECORATOR_NAME, schema.getKey(), term)) return true;
                    for (String value : schema.getValue()) {
                        if (matchField(FIELD_DECORATOR_VALUE, value, term)) return true;
                    }
                }
            }
        }

        // nothing matched the query term
        return false;
    }

    private boolean matchField(String field, String value, NexusQueryTerm term) {
        return term.matchesField(field) && term.matchesTerm(value);
    }
}
