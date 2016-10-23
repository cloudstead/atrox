package histori.dao;

import histori.dao.shard.FeedShardDAO;
import histori.model.Account;
import histori.model.Feed;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.cobbzilla.util.string.StringUtil.urlDecode;

@Repository
public class FeedDAO extends ShardedEntityDAO<Feed, FeedShardDAO> {

    @Autowired private DatabaseConfiguration database;
    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("feed"); }

    public List<Feed> findByOwner(Account account) { return findByField("owner", account.getUuid()); }

    public Feed findByAccountAndName(Account account, String name) {
        return findByUniqueFields("owner", account.getUuid(), "name", name);
    }

    public Feed findByAccountAndSource(Account account, String source) {
        return findByUniqueFields("owner", account.getUuid(), "source", source);
    }

    public Feed findByAccountAndUuidOrName(Account account, String id) {
        Feed feed = findByUniqueFields("owner", account.getUuid(), "uuid", id);
        if (feed != null) return feed;
        feed = findByUniqueFields("owner", account.getUuid(), "name", id);
        if (feed != null) return feed;
        return urlDecode(id).equals(id) ? null : findByUniqueFields("owner", account.getUuid(), "name", urlDecode(id));
    }

    public List<Feed> findActive() { return findByField("active", true); }

}
