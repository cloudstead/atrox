package histori.dao.internal;

import histori.model.internal.Shard;
import histori.server.HistoriConfiguration;
import org.cobbzilla.wizard.dao.shard.ShardMapDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository public class ShardDAO extends ShardMapDAO<Shard> {

    @Autowired private HistoriConfiguration configuration;

    @Override protected int getLogicalShardCount(String shardSet) {
        return configuration.getDatabase().getLogicalShardCount(shardSet);
    }

}
