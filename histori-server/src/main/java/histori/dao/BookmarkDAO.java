package histori.dao;

import histori.dao.shard.BookmarkShardDAO;
import histori.model.Account;
import histori.model.Bookmark;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BookmarkDAO extends ShardedEntityDAO<Bookmark, BookmarkShardDAO> {

    @Autowired private DatabaseConfiguration database;
    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("bookmark"); }

    public List<Bookmark> findByOwner(Account account) { return findByField("owner", account.getUuid()); }

    public Bookmark findByAccountAndName(Account account, String name) {
        return findByUniqueFields("owner", account.getUuid(), "name", name);
    }

}
