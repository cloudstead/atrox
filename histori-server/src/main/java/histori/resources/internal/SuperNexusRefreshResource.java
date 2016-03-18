package histori.resources.internal;

import histori.dao.SuperNexusDAO;
import histori.model.SuperNexus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.SN_REFRESH_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.wizard.resources.ResourceUtil.forbidden;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(SN_REFRESH_ENDPOINT)
@Service @Slf4j
public class SuperNexusRefreshResource {

    // you have to be in the same ClassLoader to know the secret
    public static final String KEY = randomAlphanumeric(20);

    @Autowired private SuperNexusDAO superNexusDAO;

    @POST
    @Path("/{uuid}")
    public Response updateSuperNexus (@PathParam("uuid") String uuid,
                                      @QueryParam("key") String key,
                                      @Valid SuperNexus request) {
        if (key == null || !key.equals(KEY)) return forbidden();
        final SuperNexus found = superNexusDAO.findByUuid(uuid);
        copy(found, request);
        found.setDirty(false);
        superNexusDAO.update(found);
        return ok();
    }

    @DELETE
    @Path("/{uuid}")
    public Response deleteSuperNexus (@PathParam("uuid") String uuid,
                                      @QueryParam("key") String key) {
        if (key == null || !key.equals(KEY)) return forbidden();
        superNexusDAO.delete(uuid);
        return ok();
    }

}
