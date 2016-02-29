package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.NexusDAO;
import histori.dao.NexusSummaryDAO;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.support.EntityVisibility;
import histori.model.support.GeoBounds;
import histori.model.support.NexusSummary;
import histori.model.support.TimeRange;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(SEARCH_ENDPOINT)
@Service @Slf4j
public class SearchResource {

    @Autowired private NexusSummaryDAO nexusSummaryDAO;
    @Autowired private NexusDAO nexusDAO;

    @GET
    @Path(EP_QUERY +"/{from}/{to}/{north}/{south}/{east}/{west}")
    public Response findByDateRangeAndGeo(@Context HttpContext ctx,
                                          @PathParam("from") String from,
                                          @PathParam("to") String to,
                                          @PathParam("north") double north,
                                          @PathParam("south") double south,
                                          @PathParam("east") double east,
                                          @PathParam("west") double west,
                                          @QueryParam("visibility") String visibility) {

        final Account account = optionalUserPrincipal(ctx);

        final TimeRange range;
        try {
            range = new TimeRange(from, to);
        } catch (Exception e) {
            return invalid("err.timeRange.invalid", e.getMessage());
        }

        final GeoBounds bounds = new GeoBounds(north, south, east, west);
        final EntityVisibility vis = EntityVisibility.create(visibility, EntityVisibility.everyone);

        final SearchResults<NexusSummary> found = nexusSummaryDAO.search(account, vis, range, bounds);

        return ok(found);
    }

    @GET
    @Path(EP_NEXUS+"/{uuid}")
    public Response findByUuid(@Context HttpContext ctx,
                               @PathParam("uuid") String uuid) {

        final Account account = optionalUserPrincipal(ctx);
        final Nexus nexus = nexusDAO.findByUuid(uuid);
        if (nexus == null) return notFound(uuid);

        final NexusSummary summary = nexusSummaryDAO.get(uuid);
        if (summary != null && summary.getPrimary() != null &&
                (summary.getPrimary().getVisibility() == EntityVisibility.everyone ||
                        (account != null &&
                                (account.isAdmin() || summary.getPrimary().isOwner(account.getUuid())) ))) {
            return ok(summary);
        }

        return ok(NexusSummary.simpleSummary(nexus));
    }

}
