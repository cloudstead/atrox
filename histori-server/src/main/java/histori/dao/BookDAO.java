package histori.dao;

import histori.dao.shard.BookShardDAO;
import histori.model.Account;
import histori.model.Book;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BookDAO extends ShardedEntityDAO<Book, BookShardDAO> {

    @Autowired private DatabaseConfiguration database;
    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("bookmark"); }

    public List<Book> findByOwner(Account account) { return findByField("owner", account.getUuid()); }

    public Book findByAccountAndName(Account account, String name) {
        return findByUniqueFields("owner", account.getUuid(), "name", name);
    }

    @Override public Book findByName(String name) { return findByName(name, true); }

    @Override public Book findByName(String name, boolean useCache) {
        final Book found = findByUniqueField("shortName", name, useCache);
        return found != null ? found : super.findByName(name, useCache);
    }
}
