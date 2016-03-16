package histori.dao.search;

import histori.dao.NexusEntityFilter;
import histori.dao.shard.SuperNexusShardDAO;
import histori.model.Account;
import histori.model.SearchQuery;
import histori.model.SuperNexus;
import histori.model.support.EntityVisibility;
import histori.model.support.GeoBounds;
import histori.model.support.GlobalSortOrder;
import histori.model.support.TimeRange;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.shard.task.ShardIterator;

import java.util.ArrayList;
import java.util.List;

/**
 * Iterate over all SuperNexus names within a GeoBounds and a TimeRange
 */
@Slf4j
public class SuperNexusSummaryShardIterator extends ShardIterator<SuperNexus, SuperNexusShardDAO> {

    public static final String HSQL
            = "FROM SuperNexus n "
            + "WHERE "
            + "(   ( (n.bounds.north BETWEEN ? AND ?) AND ((n.bounds.east BETWEEN ? AND ?) OR (n.bounds.west BETWEEN ? AND ?)) ) "
            +  "OR ( (n.bounds.south BETWEEN ? AND ?) AND ((n.bounds.east BETWEEN ? AND ?) OR (n.bounds.west BETWEEN ? AND ?)) ) "
            + ") AND ( "
            + "(n.timeRange.startPoint.instant BETWEEN ? AND ?) OR (n.timeRange.endPoint.instant BETWEEN ? AND ?) "
            + ") ";
    public static final String PUBLIC_CLAUSE = " AND account IS NULL AND visibility = 'everyone'";
    public static final String PRIVATE_CLAUSE = " AND account = ? AND visibility = ?";

    private NexusEntityFilter entityFilter;

    @Getter(AccessLevel.PROTECTED) private String hsql;
    @Getter(AccessLevel.PROTECTED) private List<Object> args;

    public SuperNexusSummaryShardIterator(SuperNexusShardDAO dao,
                                          Account account,
                                          SearchQuery searchQuery,
                                          NexusEntityFilter entityFilter) {
        super(dao);
        this.entityFilter = entityFilter;

        final GeoBounds bounds = searchQuery.getBounds();
        final TimeRange range = searchQuery.getTimeRange();

        EntityVisibility visibility = searchQuery.getVisibility();
        if (visibility == null) visibility = EntityVisibility.everyone;

        GlobalSortOrder sort = searchQuery.getGlobalSortOrder();
        if (sort == null) sort = GlobalSortOrder.vote_tally;

        boolean publicOnly = visibility == EntityVisibility.everyone;

        final String sortClause;
        switch (sort) {
            case newest:              sortClause = "ctime DESC";      break;
            case oldest:              sortClause = "ctime ASC";       break;
            case up_vote:             sortClause = "up_votes DESC";   break;
            case down_vote:           sortClause = "down_votes DESC"; break;
            case vote_count:          sortClause = "vote_count DESC"; break;
            case vote_tally: default: sortClause = "tally DESC";      break;
        }
        hsql = (publicOnly ? HSQL + PUBLIC_CLAUSE : HSQL + PRIVATE_CLAUSE) + " ORDER BY " + sortClause;

        args = new ArrayList<>();
        for (int i=0; i<2; i++) {
            // geo bounds does 2 different comparisons with the same parameters in the same order
            args.add(bounds.getNorth());
            args.add(bounds.getSouth());
            args.add(bounds.getEast());
            args.add(bounds.getWest());
            args.add(bounds.getEast());
            args.add(bounds.getWest());
        }

        args.add(range.start());
        args.add(range.end());
        args.add(range.start());
        args.add(range.end());
        if (!publicOnly) {
            args.add(account.getUuid());
            args.add(visibility.name());
        }
    }

    @Override public Object filter(SuperNexus entity) { return entityFilter.isAcceptable(entity) ? entity : null; }

}
