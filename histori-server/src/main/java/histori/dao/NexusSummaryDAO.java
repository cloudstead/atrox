package histori.dao;

import histori.dao.cache.VoteSummaryDAO;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.cache.VoteSummary;
import histori.model.support.EntityVisibility;
import histori.model.support.NexusSummary;
import histori.model.support.TimeRange;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.collection.FieldTransfomer;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

import static org.apache.commons.collections.CollectionUtils.collect;

@Repository
public class NexusSummaryDAO {

    private static final int MAX_OTHER_NEXUS = 10;

    @Autowired private NexusDAO nexusDAO;
    @Autowired private NexusTagDAO nexusTagDAO;
    @Autowired private VoteSummaryDAO voteSummaryDAO;

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
            results.addResult(buildSummary(group, account, visibility));
        }

        return results;
    }

    private NexusSummary buildSummary(SortedSet<Nexus> group) { return buildSummary(group, null, EntityVisibility.everyone); }

    private NexusSummary buildSummary(SortedSet<Nexus> group, Account account, EntityVisibility visibility) {

        final NexusSummary summary = new NexusSummary();
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
            others.remove(others.size()-1);
        }
        final String[] othersArray = new String[others.size()];
        summary.setOthers((String[]) collect(others, new FieldTransfomer("name")).toArray(othersArray));

        // find top tags for

        return summary;
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
