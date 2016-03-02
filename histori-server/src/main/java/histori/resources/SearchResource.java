package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.NexusDAO;
import histori.dao.NexusSummaryDAO;
import histori.dao.SearchQueryDAO;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.SearchQuery;
import histori.model.support.EntityVisibility;
import histori.model.support.GeoBounds;
import histori.model.support.NexusSummary;
import histori.model.support.TimeRange;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.dao.SearchResults;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

import static histori.ApiConstants.*;
import static histori.model.support.EntityVisibility.everyone;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(SEARCH_ENDPOINT)
@Service @Slf4j
public class SearchResource {

    @Autowired private NexusSummaryDAO nexusSummaryDAO;
    @Autowired private NexusDAO nexusDAO;
    @Autowired private SearchQueryDAO searchQueryDAO;
    @Autowired private RedisService redisService;

    private static final long SEARCH_CACHE_TIMEOUT_SECONDS = TimeUnit.DAYS.toSeconds(1);

    @Getter(lazy=true) private final RedisService searchCache = initSearchCache();
    private RedisService initSearchCache() { return redisService.prefixNamespace(SearchResource.class.getName()); }

    @POST
    @Path(EP_QUERY)
    public Response findByDateRangeAndGeo(@Context HttpContext ctx,
                                          @Valid SearchQuery query){

        final Account account = optionalUserPrincipal(ctx);

        // it must pass validation and be anonymously recorded in order to proceed
        searchQueryDAO.create(new SearchQuery(query));

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
        searchQueryDAO.create(new SearchQuery(q));

        return search(account, q);
    }

    private Response search(Account account, SearchQuery q) {

        final String cacheKey = (account == null ? "null" : account.getUuid()) + ":" + q.hashCode();
        final String json = getSearchCache().get(cacheKey);
        SearchResults<NexusSummary> results = null;
        if (!empty(json)) {
            try {
                results = fromJson(json, SearchResults.jsonType(NexusSummary.class));
            } catch (Exception e) {
                log.error("Error reading JSON under cache key ("+cacheKey+"): "+json+": "+e);
                getSearchCache().del(cacheKey);
            }
        }

        if (results == null) {
            results = _search(account, q);
            try {
                getSearchCache().set(cacheKey, toJson(results), "EX", SEARCH_CACHE_TIMEOUT_SECONDS);
            } catch (Exception e) {
                log.error("Error encoding JSON for search cache: "+results+": "+e);
            }
        }
        return ok(results);
    }

    private SearchResults<NexusSummary> _search(Account account, SearchQuery q) {
        SearchResults<NexusSummary> results;
        final TimeRange range;
        try {
            range = new TimeRange(q.getFrom(), q.getTo());
        } catch (Exception e) {
            throw invalidEx("err.timeRange.invalid", e.getMessage());
        }

        final GeoBounds bounds = new GeoBounds(q.getNorth(), q.getSouth(), q.getEast(), q.getWest());

        results = nexusSummaryDAO.search(account, q.getVisibility(), range, bounds, q.getQuery());
        return results;
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
