package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.archive.ArchiveDAO;
import histori.model.Account;
import histori.server.HistoriConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.ARCHIVES_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(ARCHIVES_ENDPOINT)
@Service @Slf4j
public class ArchivesResource {

    @Autowired private HistoriConfiguration configuration;

    @GET
    @Path("/{type}/{id}")
    public Response findArchives (@Context HttpContext ctx,
                                  @PathParam("type") String type,
                                  @PathParam("id") String id) {

        final Account account = userPrincipal(ctx);

        final ArchiveDAO dao;
        try {
            dao = (ArchiveDAO) configuration.getDaoForArchiveClass(type);
        } catch (Exception e) {
            return notFound(type);
        }

        return ok(dao.findArchives(account, id));
    }

}
