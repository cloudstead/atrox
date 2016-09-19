package histori.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.core.HttpContext;
import histori.dao.AccountDAO;
import histori.dao.SpecialAuthorDAO;
import histori.model.Account;
import histori.model.SpecialAuthorEntity;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

public abstract class SpecialAuthorsResource<E extends SpecialAuthorEntity,
                                             D extends SpecialAuthorDAO<E, S>,
                                             S extends SingleShardDAO<E>> {

    @Autowired protected AccountDAO accountDAO;
    protected abstract D getSpecialAuthorDAO();

    @GET
    public Response getAllSpecialAuthors (@Context HttpContext ctx) {
        final Account account = userPrincipal(ctx);
        return ok(getSpecialAuthorDAO().findByOwner(account));
    }

    @GET
    @Path("/{id}")
    public Response getSpecialAuthor (@Context HttpContext ctx,
                                       @PathParam("id") String id) {
        final Account account = userPrincipal(ctx);
        return ok(getSpecialAuthor(account, id));
    }

    protected E getSpecialAuthor(Account account, @PathParam("id") String id) {
        E specialAuthor = getSpecialAuthorDAO().findByAccountAndUuid(account, id);
        if (specialAuthor == null) {
            final Account block = accountDAO.findByNameOrEmail(id);
            if (block == null) throw notFoundEx(id);
            specialAuthor = getSpecialAuthorDAO().findByAccountAndAuthor(account, block.getUuid());
            if (specialAuthor == null) throw notFoundEx(id);
        }
        return specialAuthor;
    }

    @POST
    @Path("/{id}/toggleActive")
    public Response addOrUpdateSpecialAuthor (@Context HttpContext ctx,
                                              @PathParam("id") String id) {
        final Account account = userPrincipal(ctx);
        final E specialAuthor = getSpecialAuthor(account, id);
        return ok(getSpecialAuthorDAO().update((E) specialAuthor.toggleActive()));
    }

    @PUT
    public Response addOrUpdateSpecialAuthor (@Context HttpContext ctx,
                                              JsonNode jsonNode) {
        final Account account = userPrincipal(ctx);

        final E request = json(jsonNode, getSpecialAuthorDAO().getEntityClass());
        final Account toAdd = accountDAO.findByNameOrEmail(request.getName());
        if (toAdd == null) return notFound(request.getName());

        E specialAuthor = getSpecialAuthorDAO().findByAccountAndAuthor(account, toAdd.getUuid());
        if (specialAuthor != null) {
            // only priority and active flag can be updated
            specialAuthor.update(request);
            specialAuthor = getSpecialAuthorDAO().update(specialAuthor);
        } else {
            specialAuthor = instantiate(getSpecialAuthorDAO().getEntityClass());
            specialAuthor.update(request);
            specialAuthor.setSpecialAuthor(toAdd.getUuid()).setOwner(account.getUuid());
            specialAuthor = getSpecialAuthorDAO().create(specialAuthor);
        }
        return ok(specialAuthor);
    }

    @DELETE
    @Path("/{id}")
    public Response removeSpecialAuthor (@Context HttpContext ctx,
                                         @PathParam("id") String id) {

        final Account account = userPrincipal(ctx);

        final E specialAuthor = getSpecialAuthor(account, id);
        if (specialAuthor == null) return notFound(id);

        getSpecialAuthorDAO().delete(specialAuthor.getUuid());
        return ok();
    }

}
