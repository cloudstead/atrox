package histori.resources.internal;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.internal.ShardDAO;
import histori.model.Account;
import histori.model.internal.Shard;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.resources.AbstractShardsResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static histori.ApiConstants.SHARDS_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(SHARDS_ENDPOINT)
@Service @Slf4j
public class ShardsResource extends AbstractShardsResource<Shard, Account> {

    @Autowired @Getter private ShardDAO shardDAO;

    @Override protected boolean isAuthorized(HttpContext ctx, Account account) { return account.isAdmin(); }

}
