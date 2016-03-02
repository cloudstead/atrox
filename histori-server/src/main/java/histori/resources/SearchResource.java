package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.NexusDAO;
import histori.dao.NexusSummaryDAO;
import histori.dao.SearchDAO;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.Search;
import histori.model.support.*;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.*;
import static histori.model.support.EntityVisibility.everyone;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(SEARCH_ENDPOINT)
@Service @Slf4j
public class SearchResource {

    @Autowired private NexusSummaryDAO nexusSummaryDAO;
    @Autowired private NexusDAO nexusDAO;
    @Autowired private SearchDAO searchDAO;

    @POST
    @Path(EP_QUERY)
    public Response findByDateRangeAndGeo(@Context HttpContext ctx,
                                          @Valid SearchQuery query){

        final Account account = optionalUserPrincipal(ctx);

        // it must pass validation and be anonymously recorded in order to proceed
        searchDAO.create(new Search(query));

        return search(account, query);
    }

    @GET
    @Path(EP_QUERY +"/{from}/{to}/{north}/{south}/{east}/{west}")
    public Response findByDateRangeAndGeo(@Context HttpContext ctx,
                                          @PathParam("from") String from,
                                          @PathParam("to") String to,
                                          @PathParam("north") double north,
                                          @PathParam("south") double south,
                                          @PathParam("east") double east,
                                          @PathParam("west") double west,
                                          @QueryParam("q") String query,
                                          @QueryParam("v") String visibility) {

        final Account account = optionalUserPrincipal(ctx);

        final SearchQuery q = new SearchQuery()
                .setQuery(query)
                .setVisibility(EntityVisibility.create(visibility, everyone))
                .setFrom(from)
                .setTo(to)
                .setNorth(north)
                .setSouth(south)
                .setEast(east)
                .setWest(west);

        // it must pass validation and be anonymously recorded in order to proceed
        searchDAO.create(new Search(q));

        return search(account, q);
    }

    public Response search(Account account, SearchQuery q) {
        final TimeRange range;
        try {
            range = new TimeRange(q.getFrom(), q.getTo());
        } catch (Exception e) {
            return invalid("err.timeRange.invalid", e.getMessage());
        }

        final GeoBounds bounds = new GeoBounds(q.getNorth(), q.getSouth(), q.getEast(), q.getWest());

        final SearchResults<NexusSummary> found = nexusSummaryDAO.search(account, q.getVisibility(), range, bounds, q.getQuery());

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
                (summary.getPrimary().getVisibility() == everyone ||
                        (account != null &&
                                (account.isAdmin() || summary.getPrimary().isOwner(account.getUuid())) ))) {
            return ok(summary);
        }

        return ok(NexusSummary.simpleSummary(nexus));
    }

}
