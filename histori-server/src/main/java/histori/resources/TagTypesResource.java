package histori.resources;

import histori.dao.TagTypeDAO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.TAG_TYPES_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(TAG_TYPES_ENDPOINT)
@Service @Slf4j
public class TagTypesResource {

    @Autowired private TagTypeDAO tagTypeDAO;

    @GET
    public Response findAll () {
        return ok(tagTypeDAO.findAll());
    }

}
