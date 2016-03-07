package histori.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.core.HttpContext;
import histori.dao.PermalinkDAO;
import histori.model.Account;
import histori.model.Permalink;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.PERMALINKS_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(PERMALINKS_ENDPOINT)
@Service @Slf4j
public class PermalinksResource {

    @Autowired private PermalinkDAO permalinkDAO;

    @GET
    @Path("/{linkId}")
    public Response getLinkContents(@Context HttpContext ctx,
                                    @PathParam("linkId") String linkId) {

        final Account account = optionalUserPrincipal(ctx);

        final Permalink permalink = permalinkDAO.findByName(linkId);
        if (permalink == null) return notFound(linkId);
        try {
            return ok(fromJson(permalink.getJson(), JsonNode.class));
        } catch (Exception e) {
            log.error("getLinkContents: invalid JSON in permalink: "+permalink);
            return invalid("err.permalink.json.invalid");
        }
    }

}
