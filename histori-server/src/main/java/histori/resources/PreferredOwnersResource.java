package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.AccountDAO;
import histori.dao.PreferredOwnerDAO;
import histori.model.Account;
import histori.model.PreferredOwner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.PREFERRED_OWNERS_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(PREFERRED_OWNERS_ENDPOINT)
@Service @Slf4j
public class PreferredOwnersResource {

    @Autowired private AccountDAO accountDAO;
    @Autowired private PreferredOwnerDAO preferredOwnerDAO;

    @GET
    public Response getAllPreferredOwners (@Context HttpContext ctx) {
        final Account account = userPrincipal(ctx);
        return ok(preferredOwnerDAO.findByOwner(account));
    }

    @GET
    @Path("/{id}")
    public Response getPreferredOwner (@Context HttpContext ctx,
                                       @PathParam("id") String id) {
        final Account account = userPrincipal(ctx);
        return ok(getPreferredOwner(account, id));
    }

    protected PreferredOwner getPreferredOwner(Account account, @PathParam("id") String id) {
        PreferredOwner preferredOwner = preferredOwnerDAO.findByAccountAndUuid(account, id);
        if (preferredOwner == null) {
            final Account block = accountDAO.findByNameOrEmail(id);
            if (block == null) throw notFoundEx(id);
            preferredOwner = preferredOwnerDAO.findByAccountAndPreferred(account, block.getUuid());
            if (preferredOwner == null) throw notFoundEx(id);
        }
        return preferredOwner;
    }

    @PUT
    @Path("/{name}")
    public Response addPreferredOwner (@Context HttpContext ctx,
                                       @PathParam("name") String name) {
        final Account account = userPrincipal(ctx);

        final Account toBlock = accountDAO.findByNameOrEmail(name);
        if (toBlock == null) return notFound(name);

        PreferredOwner preferredOwner = preferredOwnerDAO.findByAccountAndUuid(account, toBlock.getUuid());
        if (preferredOwner != null) return ok(preferredOwner);

        preferredOwner = (PreferredOwner) new PreferredOwner()
                .setPreferred(toBlock.getUuid())
                .setOwner(account.getUuid());

        return ok(preferredOwnerDAO.create(preferredOwner));
    }

    @DELETE
    @Path("/{id}")
    public Response removePreferredOwner (@Context HttpContext ctx,
                                          @PathParam("id") String id) {

        final Account account = userPrincipal(ctx);

        final PreferredOwner preferredOwner = getPreferredOwner(account, id);
        if (preferredOwner == null) return notFound(id);

        preferredOwnerDAO.delete(preferredOwner.getUuid());
        return ok();
    }

}
