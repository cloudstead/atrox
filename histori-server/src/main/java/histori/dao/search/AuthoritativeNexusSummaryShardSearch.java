package histori.dao.search;

import histori.model.Account;
import histori.model.SearchQuery;
import histori.model.support.SearchSortOrder;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.sql.ObjectSQLQuery;

@Slf4j
public class AuthoritativeNexusSummaryShardSearch extends NexusSummaryShardSearchBase {

    public AuthoritativeNexusSummaryShardSearch(Account account,
                                                SearchQuery searchQuery,
                                                NexusSearchResults nexusResults) {
        super(account, searchQuery, nexusResults);
    }

    @Override public String entityName() { return "nexus"; }

    @Override public boolean isSql() { return true; }

    @Override public String whereClause(Account account, SearchQuery searchQuery) {
        final StringBuilder b = new StringBuilder("$authoritative = true AND $visibility = 'everyone'");
        if (searchQuery.hasBlockedOwners()) {
            b.append(" AND $owner NOT IN ");
            b.append(ObjectSQLQuery.nParams(searchQuery.getBlockedOwnersList().size()));
        }
        b.append(" AND (1=1 ");
        for (NexusQueryTerm term : searchQuery.getTerms()) {
            b.append(" AND ").append(term.sqlClause());
        }
        b.append(")");
        return b.toString();
    }

    @Override public void setArgs(Account account, SearchQuery searchQuery) {
        if (searchQuery.hasBlockedOwners()) {
            args.addAll(searchQuery.getBlockedOwnersList());
        }
        for (NexusQueryTerm term : searchQuery.getTerms()) {
            term.sqlArgs(args);
        }
    }

    @Override public String orderBy(SearchQuery searchQuery) {
        final SearchSortOrder order = searchQuery.getNexusSortOrder();
        return orderBy(order);
    }

    public String orderBy(SearchSortOrder order) {
        final String newestFirst = order != SearchSortOrder.newest ? orderBy(SearchSortOrder.newest) : null;
        switch (order) {
            case up_vote: default: return "$upVotes, "  + newestFirst;
            case down_vote:        return "$downVotes, "+ newestFirst;
            case vote_count:       return "$voteCount, "+ newestFirst;
            case vote_tally:       return "$tally, "    + newestFirst;
            case newest:           return "$mtime DESC";
            case oldest:           return "$ctime";
        }
    }

}
