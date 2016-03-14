package histori.dao;

import histori.dao.cache.VoteSummaryDAO;
import histori.dao.search.NexusSearchResults;
import histori.dao.search.SuperNexusIterator;
import histori.dao.search.SuperNexusSearchTask;
import histori.dao.search.SuperNexusSearchTaskResult;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.SearchQuery;
import histori.model.cache.VoteSummary;
import histori.model.support.EntityVisibility;
import histori.model.support.NexusSummary;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.dao.AbstractRedisDAO;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;

@Repository @Slf4j
public class NexusSummaryDAO extends AbstractRedisDAO<NexusSummary> {

    @Autowired private SuperNexusDAO superNexusDAO;
    @Autowired private TagDAO tagDAO;
    @Autowired private TagTypeDAO tagTypeDAO;
    @Autowired private VoteSummaryDAO voteSummaryDAO;

    @Autowired private RedisService redisService;

    @Getter(lazy=true) private final RedisService filterCache = initFilterCache();
    private RedisService initFilterCache() { return redisService.prefixNamespace(NexusEntityFilter.class.getSimpleName(), null); }

    @Getter(lazy=true) private final RedisService nexusSummaryCache = initNexusSummaryCache();
    private RedisService initNexusSummaryCache() { return redisService.prefixNamespace("nexus-summary-cache:", null); }

    private static final long SEARCH_TIMEOUT = TimeUnit.MINUTES.toMillis(3);
    private static final int MAX_SUPER_NEXUS_THREADS = 100;
    private final BlockingQueue<Runnable> superNexusWorkQueue = new LinkedBlockingQueue<>();
    private final ThreadPoolExecutor superNexusExecutor = new ThreadPoolExecutor(MAX_SUPER_NEXUS_THREADS/2, MAX_SUPER_NEXUS_THREADS, 10, TimeUnit.MINUTES, superNexusWorkQueue);

    /**
     * Find NexusSummaries within the provided range and region
     * @param searchQuery the query
     * @return a List of NexusSummary objects
     */
    public SearchResults<NexusSummary> search(Account account, SearchQuery searchQuery) {

        final SearchResults<NexusSummary> results = new SearchResults<>();

        final Comparator<Nexus> comparator = searchQuery.getNexusComparator();
        final NexusEntityFilter entityFilter = new NexusEntityFilter(searchQuery.getQuery(), getFilterCache(), tagDAO, tagTypeDAO);
        final NexusSearchResults nexusResults = new NexusSearchResults();

        boolean ok = true;
        List<SuperNexusIterator> iterators = null;
        try {
            // find names appropriate for visibility level within the time range/geo bounds
            iterators = superNexusDAO.findNames(searchQuery.getTimeRange(),
                                                searchQuery.getBounds(),
                                                account,
                                                searchQuery.getVisibility(),
                                                searchQuery.getGlobalSortOrder());
            final List<Future<SuperNexusSearchTaskResult>> futures = new ArrayList<>();
            for (SuperNexusIterator iter : iterators) {
                // launch tasks to walk through list of names
                final SuperNexusSearchTask task = new SuperNexusSearchTask(iter, getNexusSummaryCache(), comparator, entityFilter, nexusResults);
                futures.add(superNexusExecutor.submit(nexusResults.addTask(task)));
            }

            // wait for searches to finish
            long start = now();
            while (!futures.isEmpty()) {
                final Future<SuperNexusSearchTaskResult> f = futures.get(0);
                try {
                    final SuperNexusSearchTaskResult result = f.get(200, TimeUnit.MILLISECONDS);
                    if (!result.isSuccess()) {
                        log.warn("search: task failed: " + result.getException(), result.getException());
                    }
                } catch (InterruptedException e) {
                    ok = false;
                    die("search: interrupted: " + e);

                } catch (ExecutionException e) {
                    ok = false;
                    log.warn("search: execution: " + e);
                    futures.remove(0);

                } catch (TimeoutException e) {
                    // ok, we'll try again
                }
                if (now() - start > SEARCH_TIMEOUT) {
                    // unless we have timed out
                    ok = false;
                    die("search: timeout");
                }
            }
        } finally {
            if (!ok) {
                for (SuperNexusSearchTask task : nexusResults.getTasks()) {
                    task.cancel();
                }
            }
            if (iterators != null) {
                for (SuperNexusIterator iter : iterators) {
                    try { iter.close(); } catch (Exception e) {
                        log.warn("search: error closing iterator: "+e);
                    }
                }
            }
        }

        return results;
    }

    private class NexusRankComparator implements Comparator<Nexus> {

        @Getter @Setter private EntityVisibility visibility = EntityVisibility.everyone;

        @Override public int compare(Nexus n1, Nexus n2) {
            final VoteSummary n1summary = voteSummaryDAO.get(n1.getUuid());
            final VoteSummary n2summary = voteSummaryDAO.get(n2.getUuid());

            if (n1summary == null) return n2summary == null ? 0 : -1;
            if (n2summary == null) return 1;

            if (n1summary.getTally() > n2summary.getTally()) return 1;
            if (n1summary.getTally() < n2summary.getTally()) return -1;

            // newest one wins tiebreaker
            if (n1.getCtime() == n2.getCtime()) return 0; // highly unlikely
            return (n1.getCtime() > n2.getCtime()) ? 1 : -1;
        }
    }
}
