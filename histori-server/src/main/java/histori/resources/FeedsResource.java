package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.FeedDAO;
import histori.model.Account;
import histori.model.Feed;
import histori.server.HistoriConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.FEEDS_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(FEEDS_ENDPOINT)
@Service @Slf4j
public class FeedsResource {

    @Autowired private HistoriConfiguration configuration;
    @Autowired private FeedDAO feedDAO;

    @GET
    public Response getAllFeeds (@Context HttpContext ctx) {
        final Account account = userPrincipal(ctx);
        return ok(feedDAO.findByOwner(account));
    }

    @GET
    @Path("/{name}")
    public Response getFeed (@Context HttpContext ctx,
                             @PathParam("name") String name) {
        final Account account = userPrincipal(ctx);

        final Feed feed = feedDAO.findByAccountAndName(account, name);
        if (feed == null) return notFound(name);

        return ok(feed);
    }

    @POST
    public Response addFeed (@Context HttpContext ctx,
                             @Valid Feed request) {
        final Account account = userPrincipal(ctx);

        Feed feed = feedDAO.findByAccountAndName(account, request.getName());
        if (feed != null) return invalid("err.feed.name.notUnique");

        feed = (Feed) new Feed(request).setOwner(account.getUuid());

        final Feed created = feedDAO.create(feed);
        created.setNexuses(feed.read(configuration));

        return ok(created);
    }

    @POST
    @Path("/{name}")
    public Response updateFeed (@Context HttpContext ctx,
                                @PathParam("name") String name,
                                @Valid Feed request) {
        final Account account = userPrincipal(ctx);

        final Feed feed = feedDAO.findByName(name);
        if (feed == null) return notFound(name);

        if (!feed.getOwner().equals(account.getUuid())) return forbidden();

        feed.update(request);
        feed.setNexuses(feed.read(configuration));

        return ok(feedDAO.update(feed));
    }

    @DELETE
    @Path("/{name}")
    public Response removeFeed (@Context HttpContext ctx,
                                @PathParam("name") String name) {

        final Account account = userPrincipal(ctx);
        if (!account.isAdmin()) return forbidden();

        final Feed feed = feedDAO.findByAccountAndName(account, name);

        if (feed == null) return notFound(name);

        feedDAO.delete(feed.getUuid());
        return ok();
    }

}
