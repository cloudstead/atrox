package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.AccountDAO;
import histori.dao.BookDAO;
import histori.dao.NexusDAO;
import histori.model.Account;
import histori.model.Book;
import histori.model.Nexus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import java.util.List;

import static histori.ApiConstants.BOOKS_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(BOOKS_ENDPOINT)
@Service @Slf4j
public class BooksResource {

    @Autowired private AccountDAO accountDAO;
    @Autowired private BookDAO bookDAO;
    @Autowired private NexusDAO nexusDAO;

    @GET
    public Response getAllBooks (@Context HttpContext ctx) {
        final Account account = userPrincipal(ctx);
        return ok(bookDAO.findByOwner(account));
    }

    @GET
    @Path("/{name}")
    public Response getBook (@Context HttpContext ctx,
                             @PathParam("name") String name) {
        final Account account = userPrincipal(ctx);

        final Book book = bookDAO.findByAccountAndName(account, name);
        if (book == null) return notFound(name);

        return ok(book);
    }

    @GET
    @Path("/{name}/nexuses")
    public Response getBookContents (@Context HttpContext ctx,
                                     @PathParam("name") String name) {
        final Account account = userPrincipal(ctx);

        final Book book = bookDAO.findByAccountAndName(account, name);
        if (book == null) return notFound(name);

        return ok(nexusDAO.findByBook(name));
    }

    @PUT
    @Path("/{name}")
    public Response addBook (@Context HttpContext ctx,
                             @PathParam("name") String name) {
        final Account account = userPrincipal(ctx);

        Book book = bookDAO.findByName(name);
        if (book != null) return invalid("err.book.name.notUnique");

        book = (Book) new Book()
                .setName(name)
                .setOwner(account.getUuid());

        return ok(bookDAO.create(book));
    }

    @POST
    @Path("/{name}")
    public Response updateBook (@Context HttpContext ctx,
                                @PathParam("name") String name,
                                @Valid Book request) {
        final Account account = userPrincipal(ctx);

        final Book book = bookDAO.findByName(name);
        if (book == null) return notFound(name);

        if (!book.getOwner().equals(account.getUuid())) return forbidden();

        book.setName(request.getName()); // only name can be updated
        return ok(bookDAO.update(book));
    }

    @DELETE
    @Path("/{name}")
    public Response removeBook (@Context HttpContext ctx,
                                @PathParam("name") String name) {

        final Account account = userPrincipal(ctx);
        if (!account.isAdmin()) return forbidden();

        final Book book = bookDAO.findByAccountAndName(account, name);

        if (book == null) return notFound(name);

        List<Nexus> toDelete = nexusDAO.findByBook(name);
        while (!toDelete.isEmpty()) {
            for (Nexus nexus : toDelete) {
                nexusDAO.delete(nexus.getUuid());
            }
            toDelete = nexusDAO.findByBook(name);
        }

        bookDAO.delete(book.getUuid());
        return ok();
    }

}
