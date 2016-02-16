package histori.server;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.server.RestServerBase;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;

import java.util.List;

@Slf4j
public class HistoriServer extends RestServerBase<HistoriConfiguration> {

    public static final String[] API_CONFIG_YML = {"histori-config.yml"};

    @Override protected String getListenAddress() { return LOCALHOST; }

    // args are ignored, config is loaded from the classpath
    public static void main(String[] args) throws Exception {
        final List<ConfigurationSource> configSources = getConfigurationSources();
        main(HistoriServer.class, new DbSeedListener(), configSources);
    }

    public static List<ConfigurationSource> getConfigurationSources() {
        return getStreamConfigurationSources(HistoriServer.class, API_CONFIG_YML);
    }
}
