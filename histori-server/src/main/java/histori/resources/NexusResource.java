package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.NexusDAO;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.support.EntityVisibility;
import histori.model.support.NexusRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.NEXUS_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.string.StringUtil.urlDecode;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

/**
 * Note: all "name" variables are url-encoded
 * GET    /{name}             -- find a nexus
 * GET    /{name}/{uuid}      -- find a nexus with a name and a specific version, might belong to someone else and be visible/editable if public
 * POST   /{name}             -- create a nexus version
 * POST   /{name}/{uuid}      -- create a nexus version based on another nexus's public id, plus edits supplied in the json body of the request
 * DELETE /{name}             -- delete a nexus version
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(NEXUS_ENDPOINT)
@Service @Slf4j
public class NexusResource {

    public static final String[] CREATE_FIELDS = {"name", "nexusType", "geoJson", "timeRange", "markdown", "visibility", "tagsJson"};
    public static final String[] UPDATE_FIELDS = {"geoJson", "nexusType", "timeRange", "markdown", "visibility", "tagsJson"};

    @Autowired private NexusDAO nexusDAO;

    @GET
    @Path("/{name: .+}")
    public Response find(@Context HttpContext ctx,
                         @PathParam("name") String name) {
        final Account account = userPrincipal(ctx);
        name = urlDecode(name); // no url-encoded chars allowed

        final Nexus nexus = nexusDAO.findByOwnerAndName(account, name);
        if (nexus == null) return notFound(name);
        if (!nexus.isVisibleTo(account)) notFound(name); // deleted things might not be visible
        return ok(nexus);
    }

    @GET
    @Path("/{name: .+}/{uuid}")
    public Response find(@Context HttpContext ctx,
                         @PathParam("name") String name,
                         @PathParam("uuid") String uuid) {
        final Account account = optionalUserPrincipal(ctx);
        name = urlDecode(name); // no url-encoded chars allowed

        final Nexus nexus = nexusDAO.findByUuid(uuid);
        if (nexus == null || !nexus.isVisibleTo(account)) return notFound(uuid);
        if (!nexus.getName().equals(name)) return invalid("err.name.mismatch");

        return ok(nexus);
    }

    @POST
    @Path("/{name: .+}")
    public Response create(@Context HttpContext ctx,
                           @PathParam("name") String name,
                           @Valid NexusRequest request) {

        final Account account = userPrincipal(ctx);
        name = urlDecode(name); // no url-encoded chars allowed

        if (!name.equals(request.getName())) return invalid("err.name.mismatch");

        Nexus nexus = nexusDAO.findByOwnerAndName(account, name);
        if (nexus != null) {
            copy(nexus, request, UPDATE_FIELDS);
            nexus.setOwnerAccount(account);
            nexus = nexusDAO.update(nexus);
        } else {
            nexus = new Nexus();
            copy(nexus, request, CREATE_FIELDS);
            nexus.setOwnerAccount(account);
            nexus = nexusDAO.create(nexus);
        }
        return ok(nexus);
    }

    @POST
    @Path("/{name: .+}/{uuid}")
    public Response update(@Context HttpContext ctx,
                           @PathParam("name") String name,
                           @PathParam("uuid") String uuid,
                           @Valid NexusRequest request) {

        final Account account = userPrincipal(ctx);
        name = urlDecode(name); // no url-encoded chars allowed

        if (!name.equals(request.getName())) return invalid("err.name.mismatch");

        final Nexus idNexus = nexusDAO.findByUuid(uuid);
        if (idNexus == null || !idNexus.isVisibleTo(account)) return notFound(uuid);
        if (!name.equals(idNexus.getName())) return invalid("err.name.mismatch");

        Nexus nexus = nexusDAO.findByOwnerAndName(account, name);
        if (nexus == null || !idNexus.isOwner(account)) {
            nexus = new Nexus();
            copy(nexus, idNexus, CREATE_FIELDS);
            copy(nexus, request, UPDATE_FIELDS);
            nexus.setOrigin(idNexus.getUuid());
            nexus.setOwnerAccount(account);
            nexus = nexusDAO.create(nexus);
        } else {
            copy(nexus, request, UPDATE_FIELDS);
            nexus.setOrigin(idNexus.getUuid());
            nexus.setOwnerAccount(account);
            nexus = nexusDAO.update(nexus);
        }

        return ok(nexus);
    }

    @DELETE
    @Path("/{name: .+}")
    public Response delete(@Context HttpContext ctx,
                           @PathParam("name") String name) {

        final Account account = userPrincipal(ctx);
        name = urlDecode(name); // no url-encoded chars allowed

        Nexus nexus = nexusDAO.findByOwnerAndName(account, name);
        if (nexus == null || (!account.isAdmin() && !nexus.isOwner(account))) return notFound(name);
        nexus.setVisibility(EntityVisibility.deleted);
        nexusDAO.update(nexus);

        return ok();
    }

}
