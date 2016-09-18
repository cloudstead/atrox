package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.model.support.TimeRange;
import histori.wiki.WikiDateFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.UTILS_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalid;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok;
import static org.cobbzilla.wizard.resources.ResourceUtil.ok_empty;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(UTILS_ENDPOINT)
@Service @Slf4j
public class UtilsResource {

    @POST
    @Path("/date/parse")
    public Response parseDate(@Context HttpContext ctx,
                              String dateString) {
        if (empty(dateString)) return ok_empty();
        final TimeRange range;
        try {
            range = WikiDateFormat.parse(dateString);
        } catch (Exception e) {
            return invalid("err.date.invalid", "Invalid date ("+dateString+"): "+e, dateString);
        }
        return ok(range);
    }

}