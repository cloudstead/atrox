package histori.dao;

import histori.dao.search.CachedSearchResults;
import histori.dao.search.NexusSearchResults;
import histori.dao.search.NexusSummarySearch;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.SearchQuery;
import histori.model.support.EntityVisibility;
import histori.model.support.NexusSummary;
import histori.model.support.SearchSortOrder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.system.Sleep;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.dao.AbstractRedisDAO;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Repository @Slf4j
public class NexusSummaryDAO extends AbstractRedisDAO<NexusSummary> {

    // todo: allow higher result limits and timeout
    public static final int MAX_SEARCH_RESULTS = 100;

    public static final SearchResults<NexusSummary> NO_RESULTS = new SearchResults<>();

    private static final long SEARCH_CACHE_EXPIRATION = TimeUnit.MINUTES.toMillis(10);
    private static final int MAX_CONCURRENT_SEARCHES = 10;

    @Autowired @Getter private NexusDAO nexusDAO;
    @Autowired @Getter private SuperNexusDAO superNexusDAO;
    @Autowired @Getter private TagDAO tagDAO;
    @Autowired @Getter private TagTypeDAO tagTypeDAO;

    @Autowired private RedisService redisService;

    @Getter(lazy=true) private final RedisService filterCache = initFilterCache();
    private RedisService initFilterCache() { return redisService.prefixNamespace(NexusEntityFilter.class.getSimpleName(), null); }

    @Getter(lazy=true) private final RedisService nexusSummaryCache = initNexusSummaryCache();
    private RedisService initNexusSummaryCache() { return redisService.prefixNamespace("nexus-summary-cache:", null); }

    private final ConcurrentHashMap<String, CachedSearchResults> searchCache = new ConcurrentHashMap<>();
    private final Set<String> activeSearches = new HashSet<>(MAX_CONCURRENT_SEARCHES);

    /**
     * Find NexusSummaries within the provided range and region
     * @param account The account searching
     * @param searchQuery the query
     * @return a List of NexusSummary objects
     */
    public SearchResults<NexusSummary> search(final Account account, final SearchQuery searchQuery) {

        // empty search always returns nothing
        if (empty(searchQuery.getQuery())) return NO_RESULTS;

        final String accountKey = (account != null && searchQuery.getVisibility() != EntityVisibility.everyone) ? account.getUuid() : "public";
        final String cacheKey = accountKey + ":" +searchQuery.hashCode();

        CachedSearchResults cached;
        cached = searchCache.get(cacheKey);
        if (cached == null || cached.getAge() > SEARCH_CACHE_EXPIRATION) {
            synchronized (searchCache) {
                cached = searchCache.get(cacheKey);
                if (cached == null || cached.getAge() > SEARCH_CACHE_EXPIRATION) {

                    // can we even start a brand new search? we might be too busy...
                    synchronized (activeSearches) {
                        if (activeSearches.size() >= MAX_CONCURRENT_SEARCHES) throw unavailableEx();

                        cached = new CachedSearchResults();
                        searchCache.put(cacheKey, cached);
                        activeSearches.add(cacheKey);
                        new NexusSummarySearch(this, account, searchQuery, cacheKey, cached, searchCache, activeSearches).start();
                    }
                }
            }
        }

        long start = now();
        while (!cached.hasResults() && now() - start < superNexusDAO.getShardSearchTimeout()) {
            Sleep.sleep(200);
        }
        if (!cached.hasResults()) throw timeoutEx();

        return cached.getResults().get();
    }

    /**
     * Find NexusSummary for a single nexus
     * @param account The account searching
     * @param nexus The nexus to build a summary for
     * @return the NexusSummary
     */
    public NexusSummary search(Account account, Nexus nexus, SearchSortOrder sortOrder) {

        final List<Nexus> nexusList = nexusDAO.findByNameAndVisibleToAccount(nexus.getName(), account);
        if (nexusList.isEmpty()) return null;

        final TreeSet<Nexus> sorted = new TreeSet<>(Nexus.comparator(sortOrder));
        sorted.addAll(nexusList);

        return NexusSearchResults.toNexusSummary(sorted);
    }
}
