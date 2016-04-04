package histori.dao.search;

import histori.model.Account;
import histori.model.SearchQuery;
import histori.model.support.*;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.shard.ShardSearch;

import java.math.BigInteger;
import java.util.ArrayList;

/**
 * Iterate over all SuperNexus names within a GeoBounds and a TimeRange
 */
@Slf4j
public class SuperNexusSummaryShardSearch extends ShardSearch {

    public static final String HSQL
            = "FROM SuperNexus n "
            + "WHERE "
            // one of the corners of the bounding rectangle must be within the bounds of the query
            // there are more complex polygon-overlap algorithms but would be much harder to implement against
            // a general-purpose relational database
            // todo: consider: a custom geo-optimized storage engine
            + "(   ( (n.bounds.north BETWEEN ? AND ?) AND ((n.bounds.east BETWEEN ? AND ?) OR (n.bounds.west BETWEEN ? AND ?)) ) "
            +  "OR ( (n.bounds.south BETWEEN ? AND ?) AND ((n.bounds.east BETWEEN ? AND ?) OR (n.bounds.west BETWEEN ? AND ?)) ) "
            + ") AND ( "
            // either nexus start or end must fall within query time range
            + "(n.timeRange.startPoint.instant >= ? AND n.timeRange.startPoint.instant <= ?) "
            + "OR (n.timeRange.endPoint.instant >= ? AND n.timeRange.endPoint.instant <= ?) "
            + ") ";
    public static final String PUBLIC_CLAUSE = " AND owner IS NULL AND visibility = 'everyone'";
    public static final String PRIVATE_CLAUSE = " AND owner = ? AND visibility = ?";

    public SuperNexusSummaryShardSearch(Account account,
                                        SearchQuery searchQuery,
                                        NexusSearchResults results) {
        setCollector(results);
        setComparator(NexusSummary.comparator(searchQuery.getSummarySortOrder()));

        final GeoBounds bounds = searchQuery.getBounds();
        final TimeRange range = searchQuery.getTimeRange();

        EntityVisibility visibility = searchQuery.getVisibility();
        if (visibility == null) visibility = EntityVisibility.everyone;

        boolean publicOnly = visibility == EntityVisibility.everyone;

        hsql = (publicOnly ? HSQL + PUBLIC_CLAUSE : HSQL + PRIVATE_CLAUSE) + " ORDER BY n.canonicalName";

        double north = bounds.getNorth();
        double south = bounds.getSouth();
        double east = bounds.getEast();
        double west = bounds.getWest();
        if (bounds.getSouth() < bounds.getNorth()) {
            south = bounds.getNorth();
            north = bounds.getSouth();
        }
        if (bounds.getWest() < bounds.getEast()) {
            east = bounds.getWest();
            west = bounds.getEast();
        }
        BigInteger start = range.start();
        BigInteger end = range.end();
        if (start.compareTo(end) < 0) {
            start = range.end();
            end = range.start();
        }

        args = new ArrayList<>();
        for (int i=0; i<2; i++) {
            // geo bounds does 2 different comparisons with the same parameters in the same order
            args.add(north);
            args.add(south);
            args.add(east);
            args.add(west);
            args.add(east);
            args.add(west);
        }

        args.add(end);
        args.add(start);
        args.add(end);
        args.add(start);

        if (!publicOnly) {
            args.add(account.getUuid());
            args.add(visibility.name());
        }
    }
}
