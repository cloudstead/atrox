package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.NexusDAO;
import histori.feed.FeedService;
import histori.model.Account;
import histori.model.Feed;
import histori.model.Nexus;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@SuppressWarnings("SpringJavaAutowiredMembersInspection")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@NoArgsConstructor @Slf4j
public class FeedItemsResource {

    @Autowired private NexusDAO nexusDAO;
    @Autowired private FeedService feedService;

    private Feed feed;

    @SuppressWarnings("unused")
    public FeedItemsResource (Feed feed) { this.feed = feed; }

    @GET
    public Response getItems (@Context HttpContext ctx,
                              @QueryParam("preview") boolean preview,
                              @QueryParam("save") boolean save) {
        final Account account = userPrincipal(ctx);
        final Map<String, Nexus> found = new HashMap<>();
        final List<Nexus> current = nexusDAO.findByOwnerAndFeed(account, feed);
        for (Nexus n : current) found.put(n.getCanonicalName(), n);
        if (preview || save) {
            final List<Nexus> inFeed = feedService.read(feed, save);
            for (Nexus n : inFeed) found.put(n.getCanonicalName(), n);
        }
        return ok(found.values());
    }

    @DELETE
    public Response deleteItems (@Context HttpContext ctx) {
        final Account account = userPrincipal(ctx);
        // todo: use a shard bulk update to delete them all, this 1+N solution is terribly inefficient
        for (Nexus nexus : nexusDAO.findByOwnerAndFeed(account, feed)) {
            nexusDAO.delete(nexus.getUuid());
        }
        return ok_empty();
    }

}
