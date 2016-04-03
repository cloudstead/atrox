package histori.dao.shard;

import histori.model.SuperNexus;
import histori.server.HistoriConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.shard.AbstractSingleShardDAO;
import org.cobbzilla.wizard.model.shard.ShardMap;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.TimeUnit;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@Slf4j
public class SuperNexusShardDAO extends AbstractSingleShardDAO<SuperNexus> {

    @Autowired private HistoriConfiguration configuration;

    public static final long DEFAULT_REFRESH_INTERVAL = TimeUnit.MINUTES.toMillis(5);
    protected long getRefreshInterval() { return DEFAULT_REFRESH_INTERVAL; }

    private SuperNexusRefresher refresher;

    public void forceRefresh () { refresher.forceRefresh(); }
    public long getLastRefresh () { return refresher.getLastRefresh(); }

    @Override public void initialize(ShardMap map) {
        refresher = new SuperNexusRefresher(this, getRefreshInterval());
        configuration.autowire(refresher);
        refresher.start();
        super.initialize(map);
    }

    @Override public void cleanup() {
        if (refresher != null) refresher.stop();
        super.cleanup();
    }

}
