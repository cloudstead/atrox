package histori.dao.search;

import histori.dao.NexusSummaryDAO;
import histori.model.Account;
import histori.model.SearchQuery;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public abstract class NexusSummarySearchJob implements Runnable {

    protected NexusSummaryDAO summaryDAO;
    protected Account account;
    protected SearchQuery searchQuery;
    protected String cacheKey;
    protected CachedSearchResults cached;

    protected final Map<String, CachedSearchResults> searchCache;
    protected final Map<String, NexusSummarySearchJob> activeSearches;

    protected Thread thread = null;
    @Getter protected NexusSearchResults nexusResults;

    public NexusSummarySearchJob(NexusSummaryDAO summaryDAO,
                                 Account account,
                                 SearchQuery searchQuery,
                                 String cacheKey,
                                 CachedSearchResults cached,
                                 Map<String, CachedSearchResults> searchCache,
                                 Map<String, NexusSummarySearchJob> activeSearches) {
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

    protected abstract void runSearch();

    @Override public void run() {
        try {
            runSearch();

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
