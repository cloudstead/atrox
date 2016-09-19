package histori.dao;

import histori.dao.shard.BlockedOwnerShardDAO;
import histori.model.BlockedOwner;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.stereotype.Repository;

@Repository
public class BlockedOwnerDAO extends SpecialAuthorDAO<BlockedOwner, BlockedOwnerShardDAO> {

    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("blocked-owner"); }

    @Override protected String specialAuthorField() { return "blocked"; }

}
