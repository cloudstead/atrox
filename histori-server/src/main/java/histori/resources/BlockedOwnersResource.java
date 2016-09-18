package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.AccountDAO;
import histori.dao.BlockedOwnerDAO;
import histori.model.Account;
import histori.model.BlockedOwner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.BLOCKED_OWNERS_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(BLOCKED_OWNERS_ENDPOINT)
@Service @Slf4j
    public class BlockedOwnersResource {

        @Autowired private AccountDAO accountDAO;
        @Autowired private BlockedOwnerDAO blockedOwnerDAO;

        @GET
        public Response getAllBlockedOwners (@Context HttpContext ctx) {
            final Account account = userPrincipal(ctx);
            return ok(blockedOwnerDAO.findByOwner(account));
        }

        @GET
        @Path("/{id}")
        public Response getBlockedOwner (@Context HttpContext ctx,
                                         @PathParam("id") String id) {
            final Account account = userPrincipal(ctx);
            return ok(getBlockedOwner(account, id));
        }

        protected BlockedOwner getBlockedOwner(Account account, @PathParam("id") String id) {
            BlockedOwner blockedOwner = blockedOwnerDAO.findByAccountAndUuid(account, id);
            if (blockedOwner == null) {
                final Account block = accountDAO.findByNameOrEmail(id);
                if (block == null) throw notFoundEx(id);
                blockedOwner = blockedOwnerDAO.findByAccountAndBlocked(account, block.getUuid());
                if (blockedOwner == null) throw notFoundEx(id);
            }
            return blockedOwner;
        }

        @PUT
        @Path("/{name}")
        public Response addBlockedOwner (@Context HttpContext ctx,
                                         @PathParam("name") String name) {
            final Account account = userPrincipal(ctx);

            final Account toBlock = accountDAO.findByNameOrEmail(name);
            if (toBlock == null) return notFound(name);

            BlockedOwner blockedOwner = blockedOwnerDAO.findByAccountAndUuid(account, toBlock.getUuid());
            if (blockedOwner != null) return ok(blockedOwner);

            blockedOwner = (BlockedOwner) new BlockedOwner()
                    .setBlocked(toBlock.getUuid())
                    .setOwner(account.getUuid());

            return ok(blockedOwnerDAO.create(blockedOwner));
        }

        @DELETE
        @Path("/{id}")
        public Response removeBlockedOwner (@Context HttpContext ctx,
                                            @PathParam("id") String id) {

            final Account account = userPrincipal(ctx);

            final BlockedOwner blockedOwner = getBlockedOwner(account, id);
            if (blockedOwner == null) return notFound(id);

            blockedOwnerDAO.delete(blockedOwner.getUuid());
            return ok();
        }

    }
