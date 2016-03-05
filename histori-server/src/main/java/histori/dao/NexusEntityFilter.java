package histori.dao;

import histori.model.Nexus;
import histori.model.NexusTag;
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
public class NexusEntityFilter implements EntityFilter<Nexus> {

    private static final long FILTER_CACHE_TIMEOUT_SECONDS = TimeUnit.DAYS.toSeconds(1);

    @Getter private final RedisService filterCache;
    @Getter private final NexusTagDAO nexusTagDAO;
    private final String queryHash;
    private final Set<String> terms;
    private final Map<String, Pattern> patternCache = new HashMap<>();

    private Pattern getPattern(String term) {
        Pattern pattern = patternCache.get(term);
        if (pattern == null) {
            pattern = Pattern.compile(term);
            patternCache.put(term, pattern);
        }
        return pattern;
    }

    public NexusEntityFilter(String query, RedisService filterCache, NexusTagDAO nexusTagDAO) {
        this.filterCache = filterCache;
        this.nexusTagDAO = nexusTagDAO;
        // by sorting terms alphabetically, and trimming whitespace, we prevent duplicate cache entries
        // for searches that are essentially the same
        terms = (Set<String>) (empty(query) ? Collections.emptySet() : new TreeSet<>(StringUtil.splitIntoTerms(query)));
        queryHash = sha256_hex(StringUtil.toString(terms, " "));
    }

    @Override
    public boolean isAcceptable(Nexus nexus) {

        if (empty(terms)) return true; // empty query matches everything

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

    protected boolean isMatch(Nexus nexus) {

        // must match on all search terms
        for (String term : terms) {
            final String cacheKey = nexus.getUuid() + ":term:" + sha256_hex(term);
            String cached = null;
            try {
                cached = getFilterCache().get(cacheKey);
            } catch (Exception e) {
                log.error("Error reading from NexusEntityFilter query-term cache: " + e);
            }

            if (!empty(cached)) return Boolean.valueOf(cached);

            boolean match = matchTerm(nexus, term);

            try {
                getFilterCache().set(cacheKey, String.valueOf(match), "EX", FILTER_CACHE_TIMEOUT_SECONDS);
            } catch (Exception e) {
                log.error("Error writing to NexusEntityFilter query-term cache: " + e);
            }

            if (!match) return false;
        }
        return true;
    }

    protected boolean matchTerm(Nexus nexus, String term) {
        // name match is fuzzy
        if (fuzzyMatch(nexus.getName(), term)) return true;

        // type match must be exact (ignoring case)
        if (preciseMatch(nexus.getNexusType(), term)) return true;

        // check tags
        final List<NexusTag> tags = nexusTagDAO.findByNexus(nexus.getUuid());
        for (NexusTag tag : tags) {

            // tag name match is fuzzy
            if (fuzzyMatch(tag.getTagName(), term)) return true;

            // type match must be exact (ignoring case)
            if (preciseMatch(tag.getTagType(), term)) return true;

            // all schema matches, both keys and (possibly multiple) values, are fuzzy matches
            if (tag.hasSchemaValues()) {
                for (Map.Entry<String, Set<String>> schema : tag.getSchemaValueMap().entrySet()) {
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
