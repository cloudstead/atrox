package histori.dao;

import histori.model.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.dao.EntityFilter;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;

@Slf4j
public class NexusEntityFilter implements EntityFilter<NexusView> {

    private static final long FILTER_CACHE_TIMEOUT_SECONDS = TimeUnit.MINUTES.toSeconds(1);

    @Getter private final RedisService filterCache;
    @Getter private final TagDAO tagDAO;
    @Getter private final TagTypeDAO tagTypeDAO;
    private final String queryHash;
    private final TreeSet<String> terms;
    private final Map<String, Pattern> patternCache = new HashMap<>();

    private Pattern getPattern(String term) {
        Pattern pattern = patternCache.get(term);
        if (pattern == null) {
            pattern = Pattern.compile(term);
            patternCache.put(term, pattern);
        }
        return pattern;
    }

    public NexusEntityFilter(String query, RedisService filterCache, TagDAO tagDAO, TagTypeDAO tagTypeDAO) {

        this.filterCache = filterCache;
        this.tagDAO = tagDAO;
        this.tagTypeDAO = tagTypeDAO;

        // by sorting terms alphabetically, and trimming whitespace, we prevent duplicate cache entries
        // for searches that are essentially the same
        terms = (TreeSet<String>) (empty(query) ? new TreeSet<>() : new TreeSet<>(collectTerms(query)));
        queryHash = sha256_hex(StringUtil.toString(terms, " "));
    }

    protected List<String> collectTerms(String query) {
        final List<String> rawTerms = StringUtil.splitIntoTerms(query);
        final List<String> terms = new ArrayList<>();
        for (int i=0; i<rawTerms.size(); i++) {
            final String rawTerm = rawTerms.get(i);
            if (rawTerm.length() == 2 && rawTerm.charAt(1) == ':' && rawTerms.size() > i) {
                terms.add(rawTerm+rawTerms.get(i+1));
                i++;
            } else {
                terms.add(rawTerm);
            }
        }
        return terms;
    }

    @Override public boolean isAcceptable(NexusView nexus) {

        if (empty(terms)) return false; // empty query matches nothing (note: used to match everything)

        if (terms.size() == 1 && terms.first().equals("*")) return true; // now a single '*' matches everything

        final String cacheKey = nexus.getUuid() + ":query:" + queryHash;
        String cached = null;
        try {
            cached = getFilterCache().get(cacheKey);
        } catch (Exception e) {
            log.error("Error reading from NexusEntityFilter query cache: " + e);
        }

        if (!empty(cached)) return Boolean.valueOf(cached);

        boolean match = isMatch(nexus);

        try {
            getFilterCache().set(cacheKey, String.valueOf(match), "EX", FILTER_CACHE_TIMEOUT_SECONDS);
        } catch (Exception e) {
            log.error("Error writing to NexusEntityFilter query cache: " + e);
        }

        return match;
    }

    // todo: make this a lot better. return a score or accept some other context object so we can be smarter.
    private boolean fuzzyMatch(String value, String term) {
        final Boolean match = specialMatch(value, term);
        if (match != null) return match;
        return value != null && value.toLowerCase().contains(term.toLowerCase());
    }

    private boolean preciseMatch(String value, String term) {
        final Boolean match = specialMatch(value, term);
        if (match != null) return match;
        return value != null && value.equalsIgnoreCase(term);
    }

    private Boolean specialMatch(String value, String term) {
        if (empty(value) || term.length() < 2 || term.charAt(1) != ':') return null;
        final String realTerm = term.substring(2);
        switch (term.charAt(0)) {
            case 'E': return value.equals(realTerm);
            case 'e': return value.equalsIgnoreCase(realTerm);
            case 'R': return getPattern(realTerm).matcher(value).matches();
            case 'r': return getPattern(realTerm).matcher(value).find();
            default: return null;
        }
    }

    protected boolean isMatch(NexusView nexus) {

        // must match on all search terms
        for (String term : terms) {
            final String cacheKey = nexus.getUuid() + ":term:" + sha256_hex(term);
            String cached = null;
            try {
                cached = getFilterCache().get(cacheKey);
            } catch (Exception e) {
                log.error("Error reading from NexusEntityFilter query-term cache: " + e);
            }

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

    private void setCacheKey(String cacheKey, boolean match) {
        try {
            getFilterCache().set(cacheKey, String.valueOf(match), "EX", FILTER_CACHE_TIMEOUT_SECONDS);
        } catch (Exception e) {
            log.error("Error writing to NexusEntityFilter query-term cache: " + e);
        }
    }

    protected boolean matchTerm(NexusView nexus, String term) {
        // name match is fuzzy
        if (fuzzyMatch(nexus.getName(), term)) return true;

        // type match must be exact (ignoring case)
        if (nexus.hasNexusType()) {
            final Tag tag = tagDAO.findByCanonicalName(nexus.getNexusType());
            if (tag != null && preciseMatch(tag.getName(), term)) return true;
        }

        if (!nexus.hasTags()) return false;

        // check nexusTags
        final List<NexusTag> nexusTags = nexus.getTags();
        for (NexusTag nexusTag : nexusTags) {

            // tag name match is fuzzy
            final Tag tag = tagDAO.findByCanonicalName(nexusTag.getTagName());
            if (tag != null && fuzzyMatch(tag.getName(), term)) return true;

            // type match must be exact (ignoring case)
            final TagType tagType = tagTypeDAO.findByCanonicalName(nexusTag.getTagType());
            if (tagType != null && preciseMatch(tagType.getName(), term)) return true;

            // all schema matches, both keys and (possibly multiple) values, are fuzzy matches
            if (nexusTag.hasSchemaValues()) {
                for (Map.Entry<String, TreeSet<String>> schema : nexusTag.getSchemaValueMap().allEntrySets()) {
                    if (fuzzyMatch(schema.getKey(), term)) return true;
                    for (String value : schema.getValue()) {
                        if (fuzzyMatch(value, term)) return true;
                    }
                }
            }
        }

        // nothing matched the query term
        return false;
    }
}
