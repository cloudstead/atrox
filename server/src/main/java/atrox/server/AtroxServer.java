package atrox.server;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.server.RestServerBase;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;

import java.util.List;

import static org.cobbzilla.util.system.CommandShell.execScript;

@Slf4j
public class AtroxServer extends RestServerBase<AtroxConfiguration> {

    public static final String[] API_CONFIG_YML = {"atrox-config.yml"};

    @Override protected String getListenAddress() { return LOCALHOST; }

    // args are ignored, config is loaded from the classpath
    public static void main(String[] args) throws Exception {
        final List<ConfigurationSource> configSources = getConfigurationSources();
        main(AtroxServer.class, configSources);
    }

    @Override protected ObjectMapper getObjectMapper() {
        // models that subclass LdapEntity need this to map their fields when Jackson is populating them
        return super.getObjectMapper()
	    .setInjectableValues(new InjectableValues.Std()
				 .addValue(LdapEntity.LDAP_CONTEXT, getConfiguration().getLdap()));
    }

    public static List<ConfigurationSource> getConfigurationSources() {
        return getStreamConfigurationSources(AtroxServer.class, API_CONFIG_YML);
    }
}
