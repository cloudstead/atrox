package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.NexusDAO;
import histori.dao.NexusSummaryDAO;
import histori.dao.NexusTagDAO;
import histori.model.Account;
import histori.model.support.NexusSummary;
import histori.model.support.TimeRange;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

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
    @Autowired private NexusSummaryDAO nexusSummaryDAO;
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

        final SearchResults<NexusSummary> found = nexusSummaryDAO.findByTimeRange(range);
        return ok(found);
    }

}
