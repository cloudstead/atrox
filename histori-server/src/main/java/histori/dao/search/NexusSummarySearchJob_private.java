package histori.dao.search;

import histori.dao.NexusSummaryDAO;
import histori.model.Account;
import histori.model.SearchQuery;
import histori.model.support.NexusSummary;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.SearchResults;

import java.util.List;
import java.util.Map;

import static histori.ApiConstants.DEFAULT_SEARCH_TIMEOUT;
import static histori.ApiConstants.MAX_SEARCH_RESULTS;
import static histori.ApiConstants.MAX_SEARCH_TIMEOUT;

@Slf4j
public class NexusSummarySearchJob_private extends NexusSummarySearchJob {

    public NexusSummarySearchJob_private(NexusSummaryDAO summaryDAO,
                                         Account account,
                                         SearchQuery searchQuery,
                                         String cacheKey,
                                         CachedSearchResults cached,
                                         Map<String, CachedSearchResults> searchCache,
                                         Map<String, NexusSummarySearchJob> activeSearches) {
        super(summaryDAO, account, searchQuery, cacheKey, cached, searchCache, activeSearches);
    }

    @Override public void runSearch () {
        final NexusEntityFilter entityFilter = new NexusEntityFilter(searchQuery.getQuery(),
                searchQuery.isUseCache() ? summaryDAO.getFilterCache() : null,
                summaryDAO.getTagDAO(),
                summaryDAO.getTagTypeDAO());

        nexusResults = new NexusSearchResults(account,
                summaryDAO.getNexusDAO(),
                searchQuery,
                entityFilter,
                MAX_SEARCH_RESULTS);

        final SuperNexusSummaryShardSearch search = new SuperNexusSummaryShardSearch(account, searchQuery, nexusResults);

        long timeout = Math.min(MAX_SEARCH_TIMEOUT, searchQuery.hasTimeout() ? searchQuery.getTimeout() : DEFAULT_SEARCH_TIMEOUT);
        search.setTimeout(timeout);

        List<NexusSummary> searchResults = summaryDAO.getSuperNexusDAO().search(search);

        for (NexusSummary summary : searchResults) {
            summary.initUuid(account, searchQuery.getVisibility());
        }

        cached.getResults().set(new SearchResults<>(searchResults));
    }

}
