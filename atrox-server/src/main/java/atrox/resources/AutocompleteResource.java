package atrox.resources;

import atrox.dao.internal.EntityPointerDAO;
import atrox.model.canonical.CanonicalEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static atrox.ApiConstants.AUTOCOMPLETE_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.string.StringUtil.splitAndTrim;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(AUTOCOMPLETE_ENDPOINT)
@Service @Slf4j
public class AutocompleteResource {

    @Autowired private EntityPointerDAO pointerDAO;

    @GET
    public Response autocomplete (@QueryParam("query") String query) {
        return autocomplete(query, null, null);
    }

    @GET
    @Path("/{include}")
    public Response autocomplete (@QueryParam("query") String query,
                                  @PathParam("include") String includeTypes) {
        return autocomplete(query, includeTypes, null);
    }

    @GET
    @Path("/{include}/-{exclude}")
    public Response autocomplete (@QueryParam("query") String query,
                                  @PathParam("include") String includeTypes,
                                  @PathParam("exclude") String excludeTypes) {

        final String canonical = CanonicalEntity.canonicalize(query);
        return ok(pointerDAO.findByCanonicalNameStartsWith(canonical, splitAndTrim(includeTypes, "_"), splitAndTrim(excludeTypes, "_")));
    }
}
