package histori.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.core.HttpContext;
import histori.dao.AccountDAO;
import histori.dao.BookmarkDAO;
import histori.dao.PermalinkDAO;
import histori.model.Account;
import histori.model.Bookmark;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.BOOKMARKS_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(BOOKMARKS_ENDPOINT)
@Service @Slf4j
public class BookmarksResource {

    @Autowired private AccountDAO accountDAO;
    @Autowired private BookmarkDAO bookmarkDAO;
    @Autowired private PermalinkDAO permalinkDAO;

    @GET
    public Response getAllBookmarks (@Context HttpContext ctx) {
        final Account account = userPrincipal(ctx);
        return ok(bookmarkDAO.findByOwner(account));
    }

    @GET
    @Path("/{name}")
    public Response getBookmark (@Context HttpContext ctx,
                                 @PathParam("name") String name) {
        final Account account = userPrincipal(ctx);

        final Bookmark bookmark = bookmarkDAO.findByAccountAndName(account, name);
        if (bookmark == null) return notFound(name);

        return ok(bookmark);
    }

    @PUT
    @Path("/{name}")
    public Response addBookmark (@Context HttpContext ctx,
                                 @PathParam("name") String name,
                                 JsonNode node) {
        final Account account = userPrincipal(ctx);
        final String json = getJson(node);

        Bookmark bookmark = bookmarkDAO.findByAccountAndName(account, name);
        if (bookmark != null) return invalid("err.bookmark.name.notUnique");

        bookmark = (Bookmark) new Bookmark()
                .setName(name)
                .setJson(json)
                .setOwner(account.getUuid());

        return ok(bookmarkDAO.create(bookmark));
    }

    @POST
    @Path("/{name}")
    public Response updateBookmark (@Context HttpContext ctx,
                                    @PathParam("name") String name,
                                    JsonNode node) {
        final Account account = userPrincipal(ctx);
        final String json = getJson(node);

        Bookmark bookmark = bookmarkDAO.findByAccountAndName(account, name);
        if (bookmark == null) return notFound(name);

        bookmark.setJson(json);

        return ok(bookmarkDAO.update(bookmark));
    }

    protected String getJson(JsonNode node) {
        try { return toJson(node); } catch (Exception e) { throw invalidEx("err.bookmark.json.invalid"); }
    }

    @DELETE
    @Path("/{name}")
    public Response removeBookmark (@Context HttpContext ctx,
                                    @PathParam("name") String name) {

        final Account account = userPrincipal(ctx);
        final Bookmark bookmark = bookmarkDAO.findByAccountAndName(account, name);

        if (bookmark == null) return notFound(name);

        bookmarkDAO.delete(bookmark.getUuid());
        return ok();
    }

    @GET
    @Path("/{name}/permalink")
    public Response getPermalink (@Context HttpContext ctx,
                                  @PathParam("name") String name) {
        Account account = userPrincipal(ctx);

        final Bookmark bookmark = bookmarkDAO.findByAccountAndName(account, name);
        if (bookmark == null) return notFound(name);

        return ok(permalinkDAO.getOrCreate(bookmark.getJson()));
    }

}
