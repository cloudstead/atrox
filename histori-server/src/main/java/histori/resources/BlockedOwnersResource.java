package histori.resources;

import histori.dao.BlockedOwnerDAO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static histori.ApiConstants.BLOCKED_OWNERS_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(BLOCKED_OWNERS_ENDPOINT)
@Service @Slf4j
public class BlockedOwnersResource extends SpecialAuthorsResource {

    @Autowired @Getter private BlockedOwnerDAO specialAuthorDAO;

}
