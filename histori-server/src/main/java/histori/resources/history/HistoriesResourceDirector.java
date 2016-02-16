package histori.resources.history;

import histori.server.HistoriConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;

import static histori.ApiConstants.HISTORIES_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(HISTORIES_ENDPOINT)
@Service @Slf4j
public class HistoriesResourceDirector {

    @Autowired private HistoriConfiguration configuration;
    @Autowired private WorldEventsResource worldEventsResource;

    @Path("/{canonicalType}")
    public HistoriesResource getHistoriesResource(@PathParam("canonicalType") String canonicalType) {
        switch (canonicalType.toLowerCase()) {
            case "worldEvent": return worldEventsResource;
            default: return HistoriesResource.getResource(canonicalType, configuration);
        }
    }

}
