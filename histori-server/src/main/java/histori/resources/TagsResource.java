package histori.resources;

import histori.ApiConstants;
import histori.dao.TagDAO;
import histori.model.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.NAME_MAXLEN;
import static histori.ApiConstants.TAGS_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalid;
import static org.cobbzilla.wizard.resources.ResourceUtil.notFound;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(TAGS_ENDPOINT)
@Service @Slf4j
public class TagsResource {

    @Autowired private TagDAO tagDAO;

    @GET
    @Path(ApiConstants.EP_TAG + "/{name: .+}")
    public Response findTag (@PathParam("name") String name) {
        if (empty(name) || name.length() > NAME_MAXLEN) return notFound();
        final Tag found = tagDAO.findByCanonicalName(name);
        return found != null ? ok(found) : notFound(name);
    }

    @POST
    @Path(ApiConstants.EP_RESOLVE)
    public Response findTags (String[] names) {
        if (names.length > 100) return invalid("err.names.tooMany");
        return ok(tagDAO.findByCanonicalNames(names));
    }

}
