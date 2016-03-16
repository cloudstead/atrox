package histori.dao;

import histori.dao.search.NexusSearchResults;
import histori.dao.search.SuperNexusSummaryShardIteratorFactory;
import histori.model.Account;
import histori.model.SearchQuery;
import histori.model.support.NexusSummary;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.dao.AbstractRedisDAO;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository @Slf4j
public class NexusSummaryDAO extends AbstractRedisDAO<NexusSummary> {

    private static final int MAX_SEARCH_RESULTS = 200;

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
     * @param searchQuery the query
     * @return a List of NexusSummary objects
     */
    public SearchResults<NexusSummary> search(final Account account, final SearchQuery searchQuery) {

        final SearchResults<NexusSummary> results = new SearchResults<>();
        final NexusEntityFilter entityFilter = new NexusEntityFilter(searchQuery.getQuery(), getFilterCache(), tagDAO, tagTypeDAO);
        final NexusSearchResults nexusResults = new NexusSearchResults(searchQuery.getNexusComparator(), MAX_SEARCH_RESULTS);

        // todo: check cache for cached value. if not cached, store result in cache
        // todo: check to see if SuperNexusDAO is already searching for this same query, if so piggyback on result

        final SuperNexusSummaryShardIteratorFactory factory
                = new SuperNexusSummaryShardIteratorFactory(account, searchQuery, entityFilter, nexusResults);
        final List<NexusSummary> searchResults = superNexusDAO.iterate(factory);

        results.setResults(searchResults);
        return results;
    }

}
