package histori.dao.search;

import histori.dao.NexusEntityFilter;
import histori.dao.shard.SuperNexusShardDAO;
import histori.model.Account;
import histori.model.SearchQuery;
import histori.model.SuperNexus;
import histori.model.support.NexusSummary;
import lombok.AllArgsConstructor;
import org.cobbzilla.wizard.dao.shard.task.ShardIterator;
import org.cobbzilla.wizard.dao.shard.task.ShardIteratorFactory;
import org.cobbzilla.wizard.dao.shard.task.ShardResultCollector;

@AllArgsConstructor
public class SuperNexusSummaryShardIteratorFactory extends ShardIteratorFactory<SuperNexus, SuperNexusShardDAO, NexusSummary> {

    private final Account account;
    private final SearchQuery searchQuery;
    private final NexusEntityFilter entityFilter;
    private final NexusSearchResults nexusResults;

    @Override protected ShardIterator<SuperNexus, SuperNexusShardDAO> newIterator(SuperNexusShardDAO dao) {
        return new SuperNexusSummaryShardIterator(dao, account, searchQuery, entityFilter);
    }

    @Override public ShardResultCollector<NexusSummary> getResultCollector() { return nexusResults; }

}
