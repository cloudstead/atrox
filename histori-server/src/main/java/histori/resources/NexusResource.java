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
import java.util.ArrayList;
import java.util.List;

import static histori.ApiConstants.EP_TAGS;
import static histori.ApiConstants.NEXUS_ENDPOINT;
import static histori.model.support.EntityVisibility.everyone;
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
 * GET    /{nameOrUuid}/tags        -- find all tags for a nexus
 * GET    /{nameOrUuid}/tags/{name} -- find all tags with name
 * PUT    /{nameOrUuid}/tags/{name} -- create a single tag by name
 * POST   /{nameOrUuid}/tags/{uuid} -- update a single tag by uuid
 * DELETE /{nameOrUuid}/tags/{uuid} -- delete a single tag by uuid
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(NEXUS_ENDPOINT)
@Service @Slf4j
public class NexusResource {

    public static final String[] CREATE_FIELDS = {"name", "nexusType", "geoJson", "timeRange", "markdown", "visibility"};
    public static final String[] UPDATE_FIELDS = {"geoJson", "nexusType", "timeRange", "markdown", "visibility"};

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

        final EntityVisibility vis = EntityVisibility.create(visibility, everyone);
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
    @Path("/{uuid: .+}")
    public Response update(@Context HttpContext ctx,
                           @PathParam("uuid") String uuid,
                           @Valid NexusRequest request) {

        final Account account = userPrincipal(ctx);

        uuid = urlDecode(uuid); // no url-encoded chars allowed
        Nexus nexus = nexusDAO.findByUuid(uuid);
        if (nexus == null) return notFound(uuid);

        if (!account.isAdmin() && !account.getUuid().equals(nexus.getOwner())) {
            // do they already have a version? update that one
            Nexus callerNexus = nexusDAO.findByOwnerAndName(account, nexus.getName());
            if (callerNexus != null) {
                callerNexus = updateNexusWithTags(account, request, nexus, callerNexus);

            } else {
                // create a new copy of the nexus for this account
                callerNexus = new Nexus();
                copy(callerNexus, request, CREATE_FIELDS);
                callerNexus.setName(nexus.getName()) // name cannot change
                        .setOrigin(nexus.getUuid())  // record where we copied from
                        .setVersion(0)               // restart version numbers for our copy
                        .setOwner(account.getUuid()) // ensure caller is the owner
                        .setUuid(null);              // mark as new nexus
                callerNexus = nexusDAO.create(callerNexus);

                // copy all tags
                for (NexusTag nexusTag : nexusTagDAO.findByNexusAndOwner(account, callerNexus.getUuid())) {
                    final NexusTag tag = (NexusTag) new NexusTag(nexusTag) // copy name/type/schema
                            .setNexus(callerNexus.getUuid()) // belongs to caller's nexus
                            .setOrigin(nexusTag.getUuid())   // where we came from
                            .setVersion(0)                   // restart versioning
                            .setOwner(account.getUuid());    // belongs to caller
                    try {
                        nexusTagDAO.create(tag);
                    } catch (Exception e) {
                        log.warn("error copying tag ("+tag+"): "+e);
                    }
                    callerNexus.addTag(tag);
                }
            }
            return ok(callerNexus);
        }

        return ok(updateNexusWithTags(account, request, nexus, nexus));
    }

    public Nexus updateNexusWithTags(Account account, @Valid NexusRequest request, Nexus nexus, Nexus callerNexus) {
        // update it
        copy(callerNexus, request, UPDATE_FIELDS);
        callerNexus.setOrigin(nexus.getUuid()); // record where we copied from
        callerNexus = nexusDAO.update(callerNexus);
        if (request.hasTags()) {
            // list of those that exist in storage but are missing from the request -> delete from storage
            final List<NexusTag> toDelete = new ArrayList<>();

            // list of those found in the request that don't exist in storage -> add to storage
            final List<NexusTag> toAdd = new ArrayList<>();

            // only look at public tags
            final List<NexusTag> existingTags = nexusTagDAO.findByNexus(account, nexus.getUuid(), everyone);
            for (NexusTag tag : existingTags) {
                boolean found = false;
                for (NexusTag requestTag : request.getTags()) {
                    if (requestTag.getUuid().equals(tag.getUuid())) {
                        // only schema values can be updated. name/type are immutable.
                        tag.setSchemaValues(requestTag.getSchemaValues());
                        nexusTagDAO.update(tag);
                        found = true;
                    } else if (!existingTags.contains(requestTag)) {
                        toAdd.add((NexusTag) new NexusTag(requestTag).setNexus(callerNexus.getUuid()));
                    }
                }
                if (!found) toDelete.add(tag);
            }

            for (NexusTag tag : toAdd) callerNexus.addTag(nexusTagDAO.create(tag));
            for (NexusTag tag : toDelete) {
                nexusTagDAO.delete(tag.getUuid());
                callerNexus.removeTag(tag.getUuid());
            }
        }
        return callerNexus;
    }

    @DELETE
    @Path("/{uuid: .+}")
    public Response delete(@Context HttpContext ctx,
                           @PathParam("uuid") String uuid) {

        final Account account = userPrincipal(ctx);

        uuid = urlDecode(uuid); // no url-encoded chars allowed
        Nexus nexus = nexusDAO.findByOwnerAndUuid(account, uuid);
        if (nexus == null) return notFound(uuid);

        if (!account.isAdmin() && !account.getUuid().equals(nexus.getOwner())) return forbidden();

        // Remove all tags, then re-add whatever is being submitted
        final List<NexusTag> existingTags = nexusTagDAO.findByNexusAndOwner(account, nexus.getUuid());
        for (NexusTag existing : existingTags) nexusTagDAO.delete(existing.getUuid());

        nexusDAO.delete(nexus.getUuid());

        return ok();
    }

}
