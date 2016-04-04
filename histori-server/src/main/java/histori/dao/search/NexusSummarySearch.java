package histori.dao.search;

import histori.dao.NexusEntityFilter;
import histori.dao.NexusSummaryDAO;
import histori.model.Account;
import histori.model.SearchQuery;
import histori.model.support.NexusSummary;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.SearchResults;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class NexusSummarySearch implements Runnable {

    private NexusSummaryDAO summaryDAO;
    private Account account;
    private SearchQuery searchQuery;
    private String cacheKey;
    private CachedSearchResults cached;

    private final Map<String, CachedSearchResults> searchCache;
    private final Set<String> activeSearches;

    private Thread thread = null;

    public NexusSummarySearch(NexusSummaryDAO summaryDAO,
                              Account account,
                              SearchQuery searchQuery,
                              String cacheKey,
                              CachedSearchResults cached,
                              Map<String, CachedSearchResults> searchCache,
                              Set<String> activeSearches) {
        this.summaryDAO = summaryDAO;
        this.account = account;
        this.searchQuery = searchQuery;
        this.cacheKey = cacheKey;
        this.cached = cached;
        this.searchCache = searchCache;
        this.activeSearches = activeSearches;
    }

    public void start() {
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.start();
    }

    public void run () {
        try {
            final NexusEntityFilter entityFilter = new NexusEntityFilter(searchQuery.getQuery(),
                                                                         summaryDAO.getFilterCache(),
                                                                         summaryDAO.getTagDAO(),
                                                                         summaryDAO.getTagTypeDAO());

            final NexusSearchResults nexusResults = new NexusSearchResults(account,
                                                                           summaryDAO.getNexusDAO(),
                                                                           searchQuery,
                                                                           entityFilter,
                                                                           NexusSummaryDAO.MAX_SEARCH_RESULTS);

            final SuperNexusSummaryShardSearch search = new SuperNexusSummaryShardSearch(account, searchQuery, nexusResults);

            List<NexusSummary> searchResults = summaryDAO.getSuperNexusDAO().search(search);

            for (NexusSummary summary : searchResults) {
                summary.initUuid(account, searchQuery.getVisibility());
            }

            cached.getResults().set(new SearchResults<>(searchResults));

        } catch (RuntimeException e) {
            log.warn("search: error (removing cache key): " + e);
            synchronized (searchCache) {
                searchCache.remove(cacheKey);
            }
            throw e;

        } finally {
            synchronized (activeSearches) {
                activeSearches.remove(cacheKey);
            }
        }
    }

}
