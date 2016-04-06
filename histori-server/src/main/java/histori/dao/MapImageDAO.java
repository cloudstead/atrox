package histori.dao;

import histori.dao.shard.MapImageShardDAO;
import histori.model.Account;
import histori.model.MapImage;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MapImageDAO extends ShardedEntityDAO<MapImage, MapImageShardDAO> {

    @Autowired private DatabaseConfiguration database;
    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("map-image"); }

    public MapImage findByOwnerAndUri(String accountUuid, String storageUri) {
        return findByUniqueFields("owner", accountUuid, "uri", storageUri);
    }

    public MapImage findByOwnerAndName(String accountUuid, String name) {
        return findByUniqueFields("owner", accountUuid, "name", name);
    }

    public List<MapImage> findByOwner(Account account) { return findByField("owner", account.getUuid()); }
}
