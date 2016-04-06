package histori.resources;

import histori.server.HistoriConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.cache.AutoRefreshingReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static histori.ApiConstants.CONFIGS_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.resources.ResourceUtil.notFound;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(CONFIGS_ENDPOINT)
@Service @Slf4j
public class ConfigsResource {

    @Autowired private HistoriConfiguration configuration;

    private final AutoRefreshingReference<Map<String, String>> configMap = new AutoRefreshingReference<Map<String, String>>() {
        @Override public Map<String, String> refresh() { return initConfig(); }
        @Override public long getTimeout() { return TimeUnit.DAYS.toMillis(1); }
    };

    private Map<String, String> initConfig() {
        final Map<String, String> configs = new HashMap<>();
        configs.put("recaptcha", configuration.getRecaptcha().getPublicKey());
        configs.put("legal.terms", configuration.getLegal().getTermsOfServiceDocument());
        configs.put("legal.privacy", configuration.getLegal().getPrivacyPolicyDocument());
        configs.put("legal.community", configuration.getLegal().getCommunityGuidelinesDocument());
        configs.put("legal.licenses", configuration.getLegal().getLicensesDocument());
        return configs;
    }

    @GET public Response getAllConfigs () { return ok(configMap.get()); }

    @GET
    @Path("/{config:.+}")
    public Response getConfig (@PathParam("config") String name,
                               @QueryParam("refresh") String refresh) {
        if (!empty(refresh) && Boolean.valueOf(refresh)) {
            configuration.getLegal().refresh();
            configMap.set(null);
        }
        name = name.replace("/", ".");
        final String value = configMap.get().get(name);
        return empty(value) ? notFound(name) : ok(value);
    }

}
