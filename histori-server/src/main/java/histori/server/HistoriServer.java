package histori.server;

import histori.server.listener.DbSeedListener;
import histori.server.listener.TagCacheWarmerListener;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.server.RestServerBase;
import org.cobbzilla.wizard.server.RestServerLifecycleListener;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.listener.FlywayShardMigrationListener;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class HistoriServer extends RestServerBase<HistoriConfiguration> {

    public static final String[] API_CONFIG_YML = {"histori-config.yml"};

    @Override protected String getListenAddress() { return LOCALHOST; }

    private static final List<RestServerLifecycleListener> listeners = Arrays.asList(new RestServerLifecycleListener[] {
            new DbSeedListener(),
            new FlywayShardMigrationListener<>(),
            new TagCacheWarmerListener()
    });

    // args are ignored, config is loaded from the classpath
    public static void main(String[] args) throws Exception {

        final List<ConfigurationSource> configSources = getConfigurationSources();

        Map<String, String> env = System.getenv();
        if (env.get("HISTORI_DATAKEY") == null) {
            // use defaults
            env = CommandShell.loadShellExports(".histori.env");
        }

        // todo: in a clustered environment, only 1 server should seed/migrate the DB upon startup
        main(args, HistoriServer.class, listeners, configSources, env);
    }

    public static List<ConfigurationSource> getConfigurationSources() {
        return getStreamConfigurationSources(HistoriServer.class, API_CONFIG_YML);
    }
}
