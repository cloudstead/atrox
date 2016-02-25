package histori.dao;

import histori.dao.cache.VoteSummaryDAO;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.cache.VoteSummary;
import histori.model.support.EntityVisibility;
import histori.model.support.NexusSummary;
import histori.model.support.TimeRange;
import histori.server.HistoriConfiguration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.collection.FieldTransfomer;
import org.cobbzilla.wizard.dao.BackgroundFetcherDAO;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.collections.CollectionUtils.collect;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;

@Repository
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
     * Find publicly-visible NexusSummaries within the provided range
     * @param range the time range to search
     * @return a List of NexusSummary objects
     */
    public SearchResults<NexusSummary> findByTimeRange(TimeRange range) { return findByTimeRange(range, null, EntityVisibility.everyone); }

    public SearchResults<NexusSummary> findByTimeRange(TimeRange range, Account account, EntityVisibility visibility) {

        final SearchResults<NexusSummary> results = new SearchResults<>();
        final List<Nexus> found = account == null ? nexusDAO.findByTimeRange(range) : nexusDAO.findByTimeRange(account, range);
        if (found.isEmpty()) return results;

        // Collect nexus by name and rank them
        // Highest rank is the one with the most upvotes
        final Map<String, SortedSet<Nexus>> rollup = new HashMap<>();
        for (Nexus nexus : found) {
            SortedSet<Nexus> matches = rollup.get(nexus.getName());
            if (matches == null) {
                matches = new TreeSet<>(new NexusRankComparator());
                rollup.put(nexus.getName(), matches);
            }
            matches.add(nexus);
        }

        for (SortedSet<Nexus> group : rollup.values()) {
            final NexusSummary summary = buildSummary(group, account, visibility);
            if (summary != null) results.addResult(summary);
        }

        return results;
    }

    private NexusSummary buildSummary(SortedSet<Nexus> group, Account account, EntityVisibility visibility) {

        if (group.size() == 0) return null;

        final String cacheKey
                = "account:" + (account == null ? "null" : account.getUuid())
                + "-" + sha256_hex(group.first().getName())
                + "-" + visibility.name();

        final Map<String, Object> ctx = new HashMap<>();
        ctx.put(CTX_ACCOUNT, account);
        ctx.put(CTX_GROUP, group);
        ctx.put(CTX_VISIBILITY, visibility);

        final NexusSummary found = get(cacheKey, ctx);
        if (found != null) {
            return found;
        }
        // create a simple summary
        return new NexusSummary().setPrimary(group.first()).setTotalCount(group.size());
    }

    @Override public int getThreadPoolSize() { return configuration.getThreadPoolSizes().get(getClass().getSimpleName()); }

    @Override protected Callable<NexusSummary> newEntityJob(String uuid, Map<String, Object> context) {
        return new NexusSummaryJob(uuid,
                (Account) context.get(CTX_ACCOUNT),
                (SortedSet<Nexus>) context.get(CTX_GROUP),
                (EntityVisibility) context.get(CTX_VISIBILITY));
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
