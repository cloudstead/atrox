package histori.dao;

import histori.dao.search.NexusSearchResults;
import histori.dao.search.SuperNexusSummaryShardSearch;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.SearchQuery;
import histori.model.support.NexusSummary;
import histori.model.support.SearchSortOrder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.dao.AbstractRedisDAO;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.TreeSet;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Repository @Slf4j
public class NexusSummaryDAO extends AbstractRedisDAO<NexusSummary> {

    private static final int MAX_SEARCH_RESULTS = 200;

    public static final SearchResults<NexusSummary> NO_RESULTS = new SearchResults<>();

    @Autowired private NexusDAO nexusDAO;
    @Autowired private SuperNexusDAO superNexusDAO;
    @Autowired private TagDAO tagDAO;
    @Autowired private TagTypeDAO tagTypeDAO;

    @Autowired private RedisService redisService;

    @Getter(lazy=true) private final RedisService filterCache = initFilterCache();
    private RedisService initFilterCache() { return redisService.prefixNamespace(NexusEntityFilter.class.getSimpleName(), null); }

    @Getter(lazy=true) private final RedisService nexusSummaryCache = initNexusSummaryCache();
    private RedisService initNexusSummaryCache() { return redisService.prefixNamespace("nexus-summary-cache:", null); }

    /**
     * Find NexusSummaries within the provided range and region
     * @param account The account searching
     * @param searchQuery the query
     * @return a List of NexusSummary objects
     */
    public SearchResults<NexusSummary> search(final Account account, final SearchQuery searchQuery) {

        // empty search always returns nothing
        if (empty(searchQuery.getQuery())) return NO_RESULTS;

        final NexusEntityFilter entityFilter = new NexusEntityFilter(searchQuery.getQuery(), getFilterCache(), tagDAO, tagTypeDAO);
        final NexusSearchResults nexusResults = new NexusSearchResults(account,
                                                                       nexusDAO,
                                                                       searchQuery,
                                                                       entityFilter,
                                                                       MAX_SEARCH_RESULTS);

        // todo: check cache for cached value. if not cached, store result in cache
        // todo: check to see if SuperNexusDAO is already searching for this same query, if so piggyback on result

        final SuperNexusSummaryShardSearch search = new SuperNexusSummaryShardSearch(account, searchQuery, nexusResults);
        final List<NexusSummary> searchResults = superNexusDAO.search(search);
        for (NexusSummary summary : searchResults) {
            summary.initUuid(account, searchQuery.getVisibility());
        }

        return new SearchResults<>(searchResults);
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
