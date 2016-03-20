package histori.dao.shard;

import histori.dao.NexusDAO;
import histori.model.Nexus;
import histori.model.SuperNexus;
import histori.model.support.EntityVisibility;
import histori.resources.internal.SuperNexusRefreshResource;
import histori.server.HistoriConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Slf4j @SuppressWarnings("SpringJavaAutowiredMembersInspection")
class SuperNexusRefresher implements Runnable {

    private SuperNexusShardDAO superNexusShardDAO;
    @Autowired private NexusDAO nexusDAO;
    @Autowired private HistoriConfiguration configuration;

    private AtomicBoolean done = new AtomicBoolean(false);

    private final AtomicLong nextRefresh;
    private final AtomicLong lastRefresh = new AtomicLong(0);
    @Getter private long refreshInterval;
    private final Thread thread;

    @Getter(lazy=true) private final ApiClientBase refreshApi = initRefreshApi();
    public ApiClientBase initRefreshApi() { return new ApiClientBase(configuration.getSuperNexusRefreshBase()); }

    public long getLastRefresh () { return lastRefresh.get(); }

    public SuperNexusRefresher(SuperNexusShardDAO superNexusShardDAO, long refreshInterval) {
        this.superNexusShardDAO = superNexusShardDAO;
        this.refreshInterval = refreshInterval;
        this.nextRefresh = new AtomicLong(now() + getRefreshInterval());
        thread = new Thread(this);
        thread.setDaemon(true);
    }

    public void start () { thread.start(); }

    @Override public void run() {
        try {
            while (!done.get()) {
                if (nextRefresh.get() < now()) {
                    refresh();
                    lastRefresh.set(now());
                    nextRefresh.set(now() + getRefreshInterval());
                }
                if (done.get()) break;
                long millis = nextRefresh.get() - now();
                if (millis > 0) {
                    try {
                        synchronized (nextRefresh) {
                            nextRefresh.wait(millis);
                        }
                    } catch (InterruptedException ie) {
                        die("run: interrupted while waiting");
                    }
                }
            }
        } catch (Exception e) {
            log.error("run: unexpected exception: " + e, e);
        } finally {
            if (!done.get()) log.error("SuperNexusRefresher.run: unexpected exit");
        }
    }

    public void stop() { done.set(true); thread.interrupt(); }

    public void forceRefresh() { synchronized (nextRefresh) { nextRefresh.set(0); nextRefresh.notify(); } }

    private void refresh() {
        final List<SuperNexus> dirty = superNexusShardDAO.findByField("dirty", true);
        for (SuperNexus n : dirty) {
            try {
                refresh(n);
            } catch (Exception e) {
                log.error("refresh: error refreshing SuperNexus (" + n.getUuid() + "/" + n.getCanonicalName() + "): " + e, e);
                if (e instanceof InterruptedException) die("refresh: interrupted"); // don't swallow these
            }
        }
    }

    private void refresh(SuperNexus superNexus) throws Exception {
        final List<Nexus> matches;
        if (superNexus.hasOwner()) {
            matches = nexusDAO.findByFields("canonicalName", superNexus.getCanonicalName(),
                                            "account", superNexus.getOwner(),
                                            "visibility", superNexus.getVisibility());
        } else {
            // only looking for public nexuses
            matches = nexusDAO.findByFields("canonicalName",
                                            superNexus.getCanonicalName(), "visibility", EntityVisibility.everyone);
        }
        final String uri = "/" + superNexus.getUuid() + "?key=" + SuperNexusRefreshResource.KEY;
        if (matches.isEmpty()) {
            getRefreshApi().delete(uri);

        } else {
            SuperNexus newSuper = null;
            for (Nexus nexus : matches) {
                if (newSuper == null) {
                    newSuper = new SuperNexus(nexus);
                    newSuper.setUuid(superNexus.getUuid());
                } else {
                    newSuper.update(nexus);
                }
            }
            getRefreshApi().post(uri, toJson(superNexus));
        }
    }

}
