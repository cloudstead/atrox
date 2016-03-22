package histori.dao;

import histori.dao.shard.PermalinkShardDAO;
import histori.model.Permalink;
import org.cobbzilla.util.cache.AutoRefreshingReference;
import org.cobbzilla.util.string.Base64;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.security.ShaUtil.sha256;

@Repository
public class PermalinkDAO extends ShardedEntityDAO<Permalink, PermalinkShardDAO> {

    public static final String STD_LINK_PREFIX = "@@";

    @Autowired private DatabaseConfiguration database;
    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("permalink"); }

    public static final int MIN_PERMALINK_CHARS = 5;

    public Permalink getOrCreate(String json) {
        final String fullHash = Base64.encodeBytes(sha256(json));
        for (int i=MIN_PERMALINK_CHARS; i<fullHash.length(); i++) {
            final String name = fullHash.substring(0, i);
            final Permalink found = findByName(name);
            if (found == null) {
                final Permalink newLink = (Permalink) new Permalink().setJson(json).setName(name);
                final Permalink permalink = create(newLink);
                if (permalink.getName().startsWith(STD_LINK_PREFIX)) standardLinks.update();
                return permalink;

            } else if (found.getJson().equals(json)) {
                return found;
            }

        }
        return die("getOrCreate: error creating ("+fullHash+") based on json: "+json);
    }

    private AutoRefreshingReference<List<Permalink>> standardLinks = new AutoRefreshingReference<List<Permalink>>() {
        @Override public long getTimeout() { return TimeUnit.HOURS.toMillis(6); }
        @Override public List<Permalink> refresh() { return findByFieldLike("name", STD_LINK_PREFIX+"%"); }
    };

    public List<Permalink> findStandardLinks() { return standardLinks.get(); }
}
