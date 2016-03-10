package histori.dao;

import histori.dao.cache.VoteSummaryDAO;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.cache.VoteSummary;
import histori.model.support.EntityVisibility;
import histori.model.support.GeoBounds;
import histori.model.support.NexusSummary;
import histori.model.support.TimeRange;
import histori.server.HistoriConfiguration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.FieldTransfomer;
import org.cobbzilla.util.collection.Mappy;
import org.cobbzilla.wizard.dao.BackgroundFetcherDAO;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.collections.CollectionUtils.collect;

@Repository @Slf4j
public class NexusSummaryDAO extends BackgroundFetcherDAO<NexusSummary> {

    private static final int MAX_OTHER_NEXUS = 10;

    @Override public long getRecalculateInterval() { return TimeUnit.MINUTES.toMillis(3); }

    public static final String CTX_ACCOUNT = "account";
    public static final String CTX_GROUP = "group";
    public static final String CTX_VISIBILITY = "visibility";

    @Autowired private NexusDAO nexusDAO;
    @Autowired private NexusTagDAO nexusTagDAO;
    @Autowired private VoteSummaryDAO voteSummaryDAO;
    @Autowired private HistoriConfiguration configuration;

    /**
     * Find NexusSummaries within the provided range and region
     * @param visibility everyone : see public stuff | owner : see your stuff (but not hidden stuff) | hidden : see your hidden stuff
     * @param range the time range to search
     * @param bounds the lat/lon boundaries of the search area
     * @param query a tag query
     * @return a List of NexusSummary objects
     */
    public SearchResults<NexusSummary> search(Account account, EntityVisibility visibility, TimeRange range, GeoBounds bounds, String query) {

        final SearchResults<NexusSummary> results = new SearchResults<>();
        final List<Nexus> found = (account == null || account.isAnonymous())
                ? nexusDAO.findByTimeRangeAndGeo(range, bounds, query)
                : nexusDAO.findByTimeRangeAndGeo(account, range, bounds, visibility, query);
        if (found.isEmpty()) return results;

        // Collect nexus by name and rank them
        // Highest rank is the one with the most upvotes
        final NexusRollup rollup = new NexusRollup(found);

        for (SortedSet<Nexus> group : rollup.allValues()) {
            final NexusSummary summary = buildSummary(group, account, visibility);
            if (summary != null) results.addResult(summary);
        }

        return results;
    }

    private NexusSummary buildSummary(SortedSet<Nexus> group, Account account, EntityVisibility visibility) {

        if (group.size() == 0) return null;

        final String cacheKey = NexusSummary.summaryUuid(group, account, visibility);

        final Map<String, Object> ctx = new HashMap<>();
        ctx.put(CTX_ACCOUNT, account);
        ctx.put(CTX_GROUP, group);
        ctx.put(CTX_VISIBILITY, visibility);

        final NexusSummary found = get(cacheKey, ctx);
        if (found != null) {
            return found;
        }
        // create a simple summary
        return NexusSummary.simpleSummary(group);
    }

    @Override public int getThreadPoolSize() { return configuration.getThreadPoolSizes().get(getClass().getSimpleName()); }

    @Override protected Callable<NexusSummary> newEntityJob(String uuid, Map<String, Object> context) {
        if (context == null) {
            final List<Nexus> found = nexusDAO.findByName(nexusDAO.findByUuid(uuid).getName());
            if (found.size() == 0) return null;
            return new NexusSummaryJob(uuid,
                    null,
                    new NexusRollup(found).allValues().iterator().next(),
                    EntityVisibility.everyone);
        } else {
            return new NexusSummaryJob(uuid,
                    (Account) context.get(CTX_ACCOUNT),
                    (SortedSet<Nexus>) context.get(CTX_GROUP),
                    (EntityVisibility) context.get(CTX_VISIBILITY));
        }
    }

    private class NexusRollup extends Mappy<String, Nexus, SortedSet<Nexus>> {
        public NexusRollup(List<Nexus> list) { for (Nexus n : list) put(n.getName(), n); }
        @Override protected SortedSet<Nexus> newCollection() { return new TreeSet<>(new NexusRankComparator()); }
    }

    @AllArgsConstructor
    class NexusSummaryJob implements Callable<NexusSummary> {

        private String cacheKey;
        private Account account;
        private SortedSet<Nexus> group;
        private EntityVisibility visibility;

        @Override public NexusSummary call() throws Exception {
            final NexusSummary summary = new NexusSummary();
            summary.setUuid(cacheKey);
            if (group.isEmpty()) return summary; // should never happen

            // set primary
            final Nexus primary = group.first();
            summary.setPrimary(primary);
            primary.setTags(nexusTagDAO.findByNexus(account, primary.getUuid(), visibility));

            // set total count
            summary.setTotalCount(group.size());

            // collect uuids of top 100 others
            final List<Nexus> others = new ArrayList<>(group);
            if (!others.isEmpty()) others.remove(0);
            while (others.size() > MAX_OTHER_NEXUS) {
                others.remove(others.size() - 1);
            }
            final String[] othersArray = new String[others.size()];
            summary.setOthers((String[]) collect(others, new FieldTransfomer("name")).toArray(othersArray));

            // todo: find top tags?
            return summary;
        }
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
