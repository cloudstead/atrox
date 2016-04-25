package histori.dao.search;

import histori.model.Account;
import histori.model.SearchQuery;
import histori.model.support.GeoBounds;
import histori.model.support.NexusSummary;
import histori.model.support.TimeRange;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.dao.shard.ShardSearch;

import java.math.BigInteger;
import java.util.ArrayList;

public abstract class NexusSummaryShardSearchBase extends ShardSearch {

    public static final String ENTITY_ALIAS = "n";

    public static final String H_NORTH = "$bounds.north";
    public static final String H_SOUTH = "$bounds.south";
    public static final String H_EAST = "$bounds.east";
    public static final String H_WEST = "$bounds.west";
    public static final String H_START = "$timeRange.startPoint.instant";
    public static final String H_END = "$timeRange.endPoint.instant";

    public static final String S_NORTH = "$north";
    public static final String S_EAST = "$east";
    public static final String S_WEST = "$west";
    public static final String S_SOUTH = "$south";
    public static final String S_START = "$start_instant";
    public static final String S_END = "$end_instant";

    public static final String HSQL_BASE
            = "WHERE "
            // one of the corners of the bounding rectangle must be within the bounds of the query
            // there are more complex polygon-overlap algorithms but would be much harder to implement against
            // a general-purpose relational database
            // todo: consider: a custom geo-optimized storage engine
            + "(   ( (" + H_NORTH + " BETWEEN ? AND ?) AND ((" + H_EAST + " BETWEEN ? AND ?) OR (" + H_WEST + " BETWEEN ? AND ?)) ) "
            +  "OR ( (" + H_SOUTH + " BETWEEN ? AND ?) AND ((" + H_EAST + " BETWEEN ? AND ?) OR (" + H_WEST + " BETWEEN ? AND ?)) ) "
            + ") AND ( "
            // either nexus start or end must fall within query time range
            +  "(" + H_START + " >= ? AND " + H_START + " <= ?) "
            + "OR (" + H_END + " >= ? AND " + H_END + " <= ?) "
            + ") ";

    public static final String SQL_BASE
            = "WHERE "
            + "(   ( (" + S_NORTH + " BETWEEN ? AND ?) AND ((" + S_EAST + " BETWEEN ? AND ?) OR (" + S_WEST + " BETWEEN ? AND ?)) ) "
            +  "OR ( (" + S_SOUTH + " BETWEEN ? AND ?) AND ((" + S_EAST + " BETWEEN ? AND ?) OR (" + S_WEST + " BETWEEN ? AND ?)) ) "
            + ") AND ( "
            +  "(" + S_START + " >= ? AND " + S_START + " <= ?) "
            + "OR (" + S_END + " >= ? AND " + S_END + " <= ?) "
            + ") ";

    public NexusSummaryShardSearchBase(Account account, SearchQuery searchQuery, NexusSearchResults results) {
        setCollector(results);
        setComparator(NexusSummary.comparator(searchQuery.getSummarySortOrder()));
        initialize(account, searchQuery);
    }

    public boolean isSql() { return false; }
    public abstract String entityName();
    public abstract String whereClause(Account account, SearchQuery searchQuery);
    public abstract void setArgs(Account account, SearchQuery searchQuery);
    public String orderBy(SearchQuery searchQuery) { return "$mtime DESC"; }

    public void initialize(Account account, SearchQuery searchQuery) {
        final GeoBounds bounds = searchQuery.getBounds();
        final TimeRange range = searchQuery.getTimeRange();

        final String whereClause = whereClause(account, searchQuery);
        if (isSql()) {
            hsql = DAO.SQL_QUERY + "SELECT * FROM " + entityName() + " AS " + ENTITY_ALIAS + " "
                    + SQL_BASE
                    + (whereClause.trim().length() == 0 ? "" : " AND " + whereClause)
                    + " ORDER BY " + orderBy(searchQuery);

        } else {
            hsql = "FROM " + entityName() + " " + ENTITY_ALIAS + " "
                    + HSQL_BASE
                    + (whereClause.trim().length() == 0 ? "" : " AND " + whereClause)
                    + " ORDER BY " + orderBy(searchQuery);
        }
        hsql = hsql.replace("$", ENTITY_ALIAS+".");

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

        setArgs(account, searchQuery);
    }

}
