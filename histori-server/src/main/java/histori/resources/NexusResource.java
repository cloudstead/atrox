package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.NexusDAO;
import histori.dao.NexusTagDAO;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.NexusTag;
import histori.model.support.EntityVisibility;
import histori.model.support.NexusRequest;
import histori.server.HistoriConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.List;

import static histori.ApiConstants.EP_TAGS;
import static histori.ApiConstants.NEXUS_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.string.StringUtil.urlDecode;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;
import static org.cobbzilla.wizard.util.SpringUtil.autowire;

/**
 * GET    /{nameOrUuid}             -- find a nexus
 * PUT    /{name}                   -- create a nexus
 * POST   /{nameOrUuid}             -- update a nexus
 * DELETE /{nameOrUuid}             -- delete a nexus
 * GET    /{nameOrUuid}/tags        -- find tags for a nexus
 * GET    /{nameOrUuid}/tags/{name} -- find a single tag by name
 * PUT    /{nameOrUuid}/tags/{name} -- create a single tag by name
 * POST   /{nameOrUuid}/tags/{name} -- update a single tag by name
 * DELETE /{nameOrUuid}/tags/{name} -- delete a single tag by name
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(NEXUS_ENDPOINT)
@Service @Slf4j
public class NexusResource {

    public static final String[] CREATE_FIELDS = {"name", "geoJson", "timeRange", "commentary", "visibility"};
    public static final String[] UPDATE_FIELDS = {"geoJson", "timeRange", "commentary", "visibility"};

    @Autowired private HistoriConfiguration configuration;
    @Autowired private NexusDAO nexusDAO;
    @Autowired private NexusTagDAO nexusTagDAO;

    @Path("/{nameOrUuid}"+ EP_TAGS)
    public NexusTagsResource tagsResource (@Context HttpContext ctx,
                                           @PathParam("nameOrUuid") String nameOrUuid) {
        // todo: cache these?
        final Account account = optionalUserPrincipal(ctx);

        nameOrUuid = urlDecode(nameOrUuid); // no url-encoded chars allowed
        final Nexus nexus = nexusDAO.findByOwnerAndNameOrUuid(account, nameOrUuid);
        if (nexus == null) throw notFoundEx(nameOrUuid);

        final NexusTagsResource tagsResource = new NexusTagsResource(nexus);
        return autowire(configuration.getApplicationContext(), tagsResource);
    }

    @GET
    @Path("/{nameOrUuid: .+}")
    public Response find(@Context HttpContext ctx,
                         @PathParam("nameOrUuid") String nameOrUuid,
                         @QueryParam("visibility") String visibility) {
        final Account account = userPrincipal(ctx);

        nameOrUuid = urlDecode(nameOrUuid); // no url-encoded chars allowed
        final Nexus found = nexusDAO.findByOwnerAndNameOrUuid(account, nameOrUuid);
        if (found == null) return notFound(nameOrUuid);

        final EntityVisibility vis = EntityVisibility.create(visibility, EntityVisibility.everyone);
        found.setTags(nexusTagDAO.findByNexus(account, found.getUuid(), vis));
        return ok(found);
    }

    @PUT
    @Path("/{name: .+}")
    public Response create(@Context HttpContext ctx,
                           @PathParam("name") String name,
                           @Valid NexusRequest request) {

        final Account account = userPrincipal(ctx);

        name = urlDecode(name); // no url-encoded chars allowed
        final Nexus found = nexusDAO.findByOwnerAndName(account, name);
        if (found != null) return invalid("err.name.notUnique");

        if (!name.equals(request.getName())) return invalid("err.name.mismatch");

        final Nexus nexus = new Nexus();
        copy(nexus, request, CREATE_FIELDS);
        nexus.setOwner(account.getUuid());
        return ok(nexusDAO.create(nexus));
    }

    @POST
    @Path("/{nameOrUuid: .+}")
    public Response update(@Context HttpContext ctx,
                           @PathParam("nameOrUuid") String nameOrUuid,
                           @Valid NexusRequest request) {

        final Account account = userPrincipal(ctx);

        nameOrUuid = urlDecode(nameOrUuid); // no url-encoded chars allowed
        Nexus nexus = nexusDAO.findByOwnerAndNameOrUuid(account, nameOrUuid);
        if (nexus == null) return notFound(nameOrUuid);

        if (!account.isAdmin() && !account.getUuid().equals(nexus.getOwner())) return forbidden();

        copy(nexus, request, UPDATE_FIELDS);
        return ok(nexusDAO.update(nexus));
    }

    @DELETE
    @Path("/{nameOrUuid: .+}")
    public Response delete(@Context HttpContext ctx,
                           @PathParam("nameOrUuid") String nameOrUuid) {

        final Account account = userPrincipal(ctx);

        nameOrUuid = urlDecode(nameOrUuid); // no url-encoded chars allowed
        Nexus nexus = nexusDAO.findByOwnerAndNameOrUuid(account, nameOrUuid);
        if (nexus == null) return notFound(nameOrUuid);

        if (!account.isAdmin() && !account.getUuid().equals(nexus.getOwner())) return forbidden();

        // Remove all tags, then re-add whatever is being submitted
        final List<NexusTag> existingTags = nexusTagDAO.findByNexusAndOwner(account, nexus.getUuid());
        for (NexusTag existing : existingTags) nexusTagDAO.delete(existing.getUuid());

        nexusDAO.delete(nexus.getUuid());

        return ok();
    }

}
