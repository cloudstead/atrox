package histori.feed;

import edu.emory.mathcs.backport.java.util.Arrays;
import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import histori.dao.AccountDAO;
import histori.dao.FeedDAO;
import histori.dao.NexusDAO;
import histori.dao.TagTypeDAO;
import histori.model.Account;
import histori.model.Feed;
import histori.model.FeedReader;
import histori.model.Nexus;
import histori.server.HistoriConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.MapBuilder;
import org.cobbzilla.util.daemon.SimpleDaemon;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@Service @Slf4j
public class FeedService extends SimpleDaemon {

    public static final long FEED_CACHE_EXPIRATION = TimeUnit.HOURS.toMillis(1);
    public static final long FEED_POLL_INTERVAL = TimeUnit.HOURS.toMillis(1);

    public FeedService() { start(); }

    @Autowired private HistoriConfiguration configuration;
    @Autowired private AccountDAO accountDAO;
    @Autowired private FeedDAO feedDAO;
    @Autowired private NexusDAO nexusDAO;
    @Autowired private TagTypeDAO tagTypeDAO;
    @Autowired private RedisService redisService;

    @Getter(lazy=true) private final RedisService feedCache = initFeedCache();
    private RedisService initFeedCache() { return redisService.prefixNamespace(FeedService.class.getName()+":feeds", null); }

    public List<Nexus> readAndSave(Feed feed) { return read(feed, true); }
    public List<Nexus> read(Feed feed) { return read(feed, false); }

    public List<Nexus> read(Feed feed, boolean save) {
        List<Nexus> items;
        final String nexusJson = getFeedCache().get(feed.getSource());
        final Account feedOwner = accountDAO.findByUuid(feed.getOwner());
        if (nexusJson == null) {
            items = getFeedReader(feed.getReader()).read(feed);
            if (feed.isActive()) getFeedCache().set(feed.getSource(), json(items), "EX", FEED_CACHE_EXPIRATION);
        } else {
            items = Arrays.asList(json(nexusJson, Nexus[].class));
        }
        if (feed.isActive() && save) {
            for (int i=0; i<items.size(); i++) {
                final Nexus nexus = items.get(i);
                nexus.setFeed(feed.getUuid());
                nexus.getTags().addTag(feed.getSource(), "Citation", MapBuilder.build("last_access", ""+  now()));
                items.set(i, nexusDAO.createOrUpdateNexus(feedOwner, nexus));
            }
        }
        return items;
    }

    private final Map<String, FeedReader> readerCache = new ConcurrentHashMap();
    private FeedReader getFeedReader(String readerClass) {
        FeedReader reader = readerCache.get(readerClass);
        if (reader == null) {
            reader = instantiate(readerClass);
            reader = configuration.autowire(reader);
            readerCache.put(readerClass, reader);
        }
        return reader;
    }

    @Override protected long getStartupDelay() { return TimeUnit.MINUTES.toMillis(5); }

    private static final Object processLock = new Object();
    @Override protected long getSleepTime() { return FEED_POLL_INTERVAL; }

    @Override protected void process() {
        synchronized (processLock) {
            for (Feed feed : feedDAO.findActive()) {
                readAndSave(feed);
            }
        }
    }
}
