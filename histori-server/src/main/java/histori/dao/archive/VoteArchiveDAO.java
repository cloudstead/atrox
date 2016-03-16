package histori.dao.archive;

import histori.model.archive.VoteArchive;
import histori.dao.ShardedEntityDAO;
import histori.dao.archive.shard.VoteArchiveShardDAO;
import histori.model.Account;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class VoteArchiveDAO extends ShardedEntityDAO<VoteArchive, VoteArchiveShardDAO> implements ArchiveDAO<VoteArchive> {

    @Autowired private DatabaseConfiguration database;
    @Override protected ShardSetConfiguration getShardConfiguration() { return database.getShard("vote-archive"); }

    @Override public List<VoteArchive> findArchives(Account account, String id) {
        return findByFields("account", account, "identifier", id);
    }

}
