package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.FeedDAO;
import histori.dao.NexusDAO;
import histori.model.Account;
import histori.model.Feed;
import histori.model.Nexus;
import histori.server.HistoriConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.api.CrudOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;

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
    @Autowired private NexusDAO nexusDAO;

    private FeedItemsResource feedItemsResource = new FeedItemsResource();
    @Path("/{id}/items")
    public FeedItemsResource getItemsResource(@Context HttpContext context,
                                              @PathParam("id") String id) {
        final FeedContext ctx = new FeedContext(context, id, CrudOperation.read);
        return feedItemsResource.forContext(configuration.getApplicationContext(), ctx.feed);
    }

    @GET
    public Response getAllFeeds (@Context HttpContext context) {
        final FeedContext ctx = new FeedContext(context);
        return ok(feedDAO.findByOwner(ctx.account));
    }

    @GET
    @Path("/{id}")
    public Response getFeed (@Context HttpContext context,
                             @PathParam("id") String id) {
        final FeedContext ctx = new FeedContext(context, id, CrudOperation.read);
        return ok(ctx.feed);
    }

    @POST
    public Response addOrUpdateFeed (@Context HttpContext context,
                                     @Valid Feed request) {
        final FeedContext ctx = new FeedContext(context);
        Feed found = feedDAO.findByAccountAndName(ctx.account, request.getName());
        if (found != null) {
            found.update(request);
            ctx.feed = feedDAO.update(found);
        } else {
            found = (Feed) new Feed(request).setOwner(ctx.account.getUuid());
            ctx.feed = feedDAO.create(found);
        }
        return ok(ctx.feed);
    }

    @DELETE
    @Path("/{id}")
    public Response removeFeed (@Context HttpContext context,
                                @PathParam("id") String id) {
        final FeedContext ctx = new FeedContext(context, id, CrudOperation.delete);
        final List<Nexus> items = nexusDAO.findByOwnerAndFeed(ctx.account, ctx.feed);
        for (Nexus nexus : items) {
            nexus.setFeed(ctx.feed.getSource());
        }

        feedDAO.delete(ctx.feed.getUuid());
        return ok();
    }

    private class FeedContext {
        public Account account;
        public Feed feed;
        public FeedContext(HttpContext ctx) { account = userPrincipal(ctx); }
        public FeedContext(HttpContext ctx, String id, CrudOperation op) {
            this(ctx);
            feed = feedDAO.findByAccountAndUuidOrName(account, id);
            if (feed == null) throw notFoundEx(id);
            if (!feed.getOwner().equals(account.getUuid()) && !account.isAdmin()) throw notFoundEx(id);
        }
    }

}
