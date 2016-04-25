package histori.dao.search;

import histori.model.Account;
import histori.model.SearchQuery;
import lombok.extern.slf4j.Slf4j;

/**
 * Iterate over all SuperNexus names within a GeoBounds and a TimeRange
 */
@Slf4j
public class SuperNexusSummaryShardSearch extends NexusSummaryShardSearchBase {

    public static final String HSQL_ENTITY = "SuperNexus";
    public static final String PUBLIC_CLAUSE = " AND $owner IS NULL AND $visibility = 'everyone'";
    public static final String PRIVATE_CLAUSE = " AND $owner = ? AND $visibility = ?";

    @Override public String entityName() { return HSQL_ENTITY; }

    @Override public String whereClause(Account account, SearchQuery searchQuery) {
        boolean privateQuery = searchQuery.hasVisibility() && !searchQuery.getVisibility().isEveryone();
        return privateQuery ? PRIVATE_CLAUSE : PUBLIC_CLAUSE;
    }

    @Override public void setArgs(Account account, SearchQuery searchQuery) {
        boolean privateQuery = searchQuery.hasVisibility() && !searchQuery.getVisibility().isEveryone();
        if (privateQuery) {
            args.add(account.getUuid());
            args.add(searchQuery.getVisibility().name());
        }
    }

    public SuperNexusSummaryShardSearch(Account account,
                                        SearchQuery searchQuery,
                                        NexusSearchResults results) {
        super(account, searchQuery, results);
    }

}
