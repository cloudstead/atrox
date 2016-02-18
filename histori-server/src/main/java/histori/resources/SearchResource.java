package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.NexusDAO;
import histori.dao.NexusTagDAO;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.support.EntityVisibility;
import histori.model.support.TimeRange;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;

import static histori.ApiConstants.EP_DATE;
import static histori.ApiConstants.SEARCH_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(SEARCH_ENDPOINT)
@Service @Slf4j
public class SearchResource {

    @Autowired private NexusDAO nexusDAO;
    @Autowired private NexusTagDAO nexusTagDAO;

    @GET
    @Path(EP_DATE+"/{from}/{to}")
    public Response findByDateRange (@Context HttpContext ctx,
                                     @PathParam("from") String from,
                                     @PathParam("to") String to,
                                     @QueryParam("visibility") String visibility) {

        final Account account = optionalUserPrincipal(ctx);

        final TimeRange range;
        try {
            range = new TimeRange(from, to);
        } catch (Exception e) {
            return invalid("err.timeRange.invalid", e.getMessage());
        }

        final List<Nexus> found = nexusDAO.findByTimeRange(account, range);
        final EntityVisibility vis = EntityVisibility.create(visibility, EntityVisibility.everyone);
        for (Nexus nexus : found) {
            nexus.setTags(nexusTagDAO.findByNexus(account, nexus.getUuid(), vis));
        }
        return ok(new SearchResults<>(found));
    }

}
