package histori.resources;

import histori.server.HistoriConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

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

    @Getter(lazy=true) private final Map<String, String> configMap = initConfig();

    private Map<String, String> initConfig() {
        final Map<String, String> configs = new HashMap<>();
        configs.put("recaptcha", configuration.getRecaptcha().getPublicKey());
        return configs;
    }

    @GET public Response getAllConfigs () { return ok(getConfigMap()); }

    @GET
    @Path("/{config}")
    public Response getConfig (@PathParam("config") String name) {
        final String value = getConfigMap().get(name);
        return empty(value) ? notFound(name) : ok(value);
    }

}
