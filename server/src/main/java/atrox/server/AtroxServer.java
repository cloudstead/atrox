package atrox.server;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.server.RestServerBase;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;

import java.util.List;

@Slf4j
public class AtroxServer extends RestServerBase<AtroxConfiguration> {

    public static final String[] API_CONFIG_YML = {"atrox-config.yml"};

    @Override protected String getListenAddress() { return LOCALHOST; }

    // args are ignored, config is loaded from the classpath
    public static void main(String[] args) throws Exception {
        final List<ConfigurationSource> configSources = getConfigurationSources();
        main(AtroxServer.class, configSources);
    }

    public static List<ConfigurationSource> getConfigurationSources() {
        return getStreamConfigurationSources(AtroxServer.class, API_CONFIG_YML);
    }
}
