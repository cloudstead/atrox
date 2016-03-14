package histori.dao.search;

import histori.dao.NexusEntityFilter;
import histori.model.Nexus;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.cache.redis.RedisService;

import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Retrieve all Nexuses with the same canonical name.
 * Sort by comparator.
 * If highest-ranked nexus matches entityFilter, add all Nexuses to
 */
@Slf4j
public class SuperNexusSearchTask implements Callable<SuperNexusSearchTaskResult> {

    private Iterator<String> names;
    private RedisService redis;
    private Comparator<Nexus> comparator;
    private NexusEntityFilter entityFilter;
    private NexusSearchResults results;

    private AtomicBoolean cancelled = new AtomicBoolean(false);

    public SuperNexusSearchTask(SuperNexusIterator names,
                                RedisService redis,
                                Comparator<Nexus> comparator,
                                NexusEntityFilter entityFilter,
                                NexusSearchResults results) {
        this.names = names;
        this.redis = redis;
        this.comparator = comparator;
        this.entityFilter = entityFilter;
        this.results = results;
    }

    public void cancel() { this.cancelled.set(true); }

    @Override public SuperNexusSearchTaskResult call() throws Exception {
        while (names.hasNext() && !cancelled.get()) {
            String name = null;
            try {
                name = names.next();
                final TreeSet<Nexus> allWithName = findNexuses(name);
                if (entityFilter.isAcceptable(allWithName.first())) {
                    results.putAll(name, allWithName);
                }
            } catch (Exception e) {
                if (name == null) {
                    log.error("error getting next nexus name: " + e);
                } else {
                    log.error("error with nexus=" + name + ": " + e);
                }
                return new SuperNexusSearchTaskResult(this, e);
            }
        }
        return new SuperNexusSearchTaskResult(this);
    }

    private TreeSet<Nexus> findNexuses(String name) {
        final TreeSet<Nexus> found = new TreeSet<>(comparator);
        String json = redis.get(name);
        // check redis
        // if not found in redis, check database, write to redis
        return found;
    }

}
