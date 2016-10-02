package histori.dao;

import histori.ApiConstants;
import histori.dao.search.*;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.NexusView;
import histori.model.SearchQuery;
import histori.model.support.NexusSummary;
import histori.model.support.SearchSortOrder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.util.system.Sleep;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.dao.AbstractRedisDAO;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.wizard.resources.ResourceUtil.timeoutEx;
import static org.cobbzilla.wizard.resources.ResourceUtil.unavailableEx;

@Repository @Slf4j
public class NexusSummaryDAO extends AbstractRedisDAO<NexusSummary> {

    public static final SearchResults<NexusSummary> NO_RESULTS = new SearchResults<>();

    @Autowired @Getter private NexusDAO nexusDAO;
    @Autowired @Getter private SuperNexusDAO superNexusDAO;
    @Autowired @Getter private TagDAO tagDAO;
    @Autowired @Getter private TagTypeDAO tagTypeDAO;

    @Autowired private RedisService redisService;

    @Getter(lazy=true) private final RedisService filterCache = initFilterCache();
    private RedisService initFilterCache() { return redisService.prefixNamespace(NexusEntityFilter.class.getSimpleName(), null); }

    @Getter(lazy=true) private final RedisService nexusSummaryCache = initNexusSummaryCache();
    private RedisService initNexusSummaryCache() { return redisService.prefixNamespace("nexus-summary-cache:", null); }

    private final Map<String, CachedSearchResults> searchCache = new ConcurrentHashMap<>();
    private final Map<String, NexusSummarySearchJobBase> activeSearches = new ConcurrentHashMap<>(ApiConstants.MAX_CONCURRENT_SEARCHES);

    /**
     * Find NexusSummaries within the provided range and region
     * @param account The account searching
     * @param searchQuery the query
     * @return a List of NexusSummary objects
     */
    public SearchResults<NexusSummary> search(final Account account, final SearchQuery searchQuery) {

        // empty search always returns nothing
        if (empty(searchQuery.getQuery())) return NO_RESULTS;

        final String cacheKey = String.valueOf(searchQuery.hashCode());

        final boolean useCache = searchQuery.isUseCache();
        CachedSearchResults cached;
        cached = useCache ? searchCache.get(cacheKey) : null;
        if (cached == null || cached.getAge() > ApiConstants.SEARCH_CACHE_EXPIRATION) {
            synchronized (searchCache) {
                cached = useCache ? searchCache.get(cacheKey) : null;
                if (cached == null || cached.getAge() > ApiConstants.SEARCH_CACHE_EXPIRATION) {

                    // can we even start a brand new search? we might be too busy...
                    synchronized (activeSearches) {
                        if (activeSearches.size() >= ApiConstants.MAX_CONCURRENT_SEARCHES) throw unavailableEx();

                        cached = new CachedSearchResults();
                        if (useCache) searchCache.put(cacheKey, cached);

                        final NexusSummarySearchJobBase job = new NexusSummarySearchJob(this, account, searchQuery, cacheKey, cached, searchCache, activeSearches);
                        activeSearches.put(cacheKey, job);
                        job.start();
                    }
                }
            }
        }

        long start = now();
        while (!cached.hasResults() && now() - start < (superNexusDAO.getShardSearchTimeout() + 2000)) {
            Sleep.sleep(200);
        }
        if (!cached.hasResults()) {
            final String prefix = "search("+cacheKey+", "+searchQuery.getQuery()+"): ";
            final NexusSummarySearchJobBase activeJob = activeSearches.get(cacheKey);
            if (activeJob != null) {
                log.warn(prefix+"timed out getting full results, returning early with " + activeJob.getNexusResults().size() + " results");
                cached.getResults().set(new SearchResults<>(activeJob.getNexusResults().getResults()));
            } else {
                log.error(prefix+"timed out getting results ");
                throw timeoutEx();
            }
        }

        return cached.getResults().get();
    }

    /**
     * Find NexusSummary for a single nexus
     * @param account The account searching
     * @param nexus The nexus to build a summary for
     * @return the NexusSummary
     */
    public NexusSummary search(Account account, Nexus nexus,
                               final List<String> preferredOwners,
                               final List<String> blockedOwners,
                               final SearchSortOrder sort) {

        final List<Nexus> nexusList = nexusDAO.findByNameAndVisibleToAccountInSearchResults(nexus.getName(), account);
        if (nexusList.isEmpty()) return null;

        final Comparator<NexusView> comparator;
        if (!empty(preferredOwners)) {
            final SearchQuery searchQuery = new SearchQuery()
                    .setBlockedOwners(StringUtil.toString(blockedOwners))
                    .setPreferredOwners(StringUtil.toString(preferredOwners))
                    .setNexusSortOrder(sort)
                    .setSummarySortOrder(sort);
            comparator = searchQuery.getNexusComparator();
        } else {
            comparator = Nexus.comparator(sort);
        }
        final TreeSet<Nexus> sorted = new TreeSet<>(comparator);
        for (Nexus found : nexusList) {
            if (blockedOwners == null || !blockedOwners.contains(found.getOwner())) {
                sorted.addAll(nexusList);
            }
        }

        return NexusSearchResults.toNexusSummary(sorted);
    }

}
