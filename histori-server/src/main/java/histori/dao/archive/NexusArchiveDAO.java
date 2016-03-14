package histori.dao.archive;

import histori.archive.NexusArchive;
import org.cobbzilla.wizard.dao.shard.AbstractShardedDAO;
import org.springframework.stereotype.Repository;

@Repository public class NexusArchiveDAO extends AbstractShardedDAO<NexusArchive, NexusArchiveShardDAO> {}
