package histori.dao.archive;

import histori.model.archive.NexusArchive;
import histori.dao.ShardedEntityDAO;
import histori.dao.archive.shard.NexusArchiveShardDAO;
import histori.model.Account;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository public class NexusArchiveDAO extends ShardedEntityDAO<NexusArchive, NexusArchiveShardDAO> implements ArchiveDAO<NexusArchive> {

    @Autowired private DatabaseConfiguration database;
    @Override protected ShardSetConfiguration getShardConfiguration() { return database.getShard("nexus-archive"); }

    @Override public List<NexusArchive> findArchives(Account account, String id) {
        return findByFields("owner", account.getUuid(), "identifier", id);
    }

}
