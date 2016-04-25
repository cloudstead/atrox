package histori.dao.search;

import histori.dao.NexusSummaryDAO;
import histori.model.Account;
import histori.model.SearchQuery;
import histori.model.support.NexusSummary;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.SearchResults;

import java.util.List;
import java.util.Map;

import static histori.ApiConstants.*;

@Slf4j
public class NexusSummarySearchJob_public extends NexusSummarySearchJob {

    public NexusSummarySearchJob_public(NexusSummaryDAO summaryDAO,
                                        Account account,
                                        SearchQuery searchQuery,
                                        String cacheKey,
                                        CachedSearchResults cached,
                                        Map<String, CachedSearchResults> searchCache,
                                        Map<String, NexusSummarySearchJob> activeSearches) {
        super(summaryDAO, account, searchQuery, cacheKey, cached, searchCache, activeSearches);
    }

    @Override public void runSearch () {
        nexusResults = new NexusSearchResults(account,
                summaryDAO.getNexusDAO(),
                searchQuery,
                null,
                MAX_SEARCH_RESULTS);

        final AuthoritativeNexusSummaryShardSearch search = new AuthoritativeNexusSummaryShardSearch(account, searchQuery, nexusResults);

        long timeout = Math.min(MAX_SEARCH_TIMEOUT, searchQuery.hasTimeout() ? searchQuery.getTimeout() : DEFAULT_SEARCH_TIMEOUT);
        search.setTimeout(timeout);

        final List<NexusSummary> results = summaryDAO.getNexusDAO().search(search);

        cached.getResults().set(new SearchResults<>(results));
    }
}
