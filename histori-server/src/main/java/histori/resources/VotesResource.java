package histori.resources;

import com.sun.jersey.api.core.HttpContext;
import histori.dao.VoteDAO;
import histori.dao.cache.VoteSummaryDAO;
import histori.model.*;
import histori.model.cache.VoteSummary;
import histori.server.HistoriConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.DAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

/**
 * Voting endpoint
 *
 * POST   /{type}/{uuid}/up      - up vote
 * POST   /{type}/{uuid}/down    - down vote
 * GET    /{type}/{uuid}         - see your vote
 * DELETE /{type}/{uuid}         - delete your vote
 * GET    /{type}/{uuid}/summary - get vote summary
 */
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(VOTES_ENDPOINT)
@Service @Slf4j
public class VotesResource {

    @Autowired private VoteDAO voteDAO;
    @Autowired private VoteSummaryDAO voteSummaryDAO;
    @Autowired private HistoriConfiguration configuration;

    @POST
    @Path("/{type}/{uuid}" + EP_UPVOTE)
    public Response upVote (@Context HttpContext ctx,
                            @PathParam("type") String type,
                            @PathParam("uuid") String uuid) {
        return ok(voteDAO.upVote((Account) userPrincipal(ctx), uuid));
    }

    @POST
    @Path("/{type}/{uuid}" + EP_DOWNVOTE)
    public Response downVote (@Context HttpContext ctx,
                              @PathParam("type") String type,
                              @PathParam("uuid") String uuid) {
        return ok(voteDAO.downVote((Account) userPrincipal(ctx), uuid));
    }

    @GET
    @Path("/{type}/{uuid}")
    public Response myVote (@Context HttpContext ctx,
                            @PathParam("type") String type,
                            @PathParam("uuid") String uuid) {
        final Vote vote = voteDAO.findByOwnerAndEntity((Account) userPrincipal(ctx), uuid);
        return vote != null ? ok(vote) : notFound(uuid);
    }

    @DELETE
    @Path("/{type}/{uuid}")
    public Response deleteMyVote (@Context HttpContext ctx,
                                  @PathParam("type") String type,
                                  @PathParam("uuid") String uuid) {
        final Vote vote = voteDAO.findByOwnerAndEntity((Account) userPrincipal(ctx), uuid);
        if (vote == null) return ok();
        voteDAO.delete(vote.getUuid());
        return ok();
    }

    @GET
    @Path("/{type}/{uuid}"+EP_SUMMARY)
    public Response getVoteSummary (@Context HttpContext ctx,
                                    @PathParam("type") String type,
                                    @PathParam("uuid") String uuid) {

        final Account account = optionalUserPrincipal(ctx);

        // what kind of thing is this?
        final Class<? extends SocialEntity> entityClass;
        switch (type.toLowerCase()) {
            case "nexus": entityClass = Nexus.class; break;
            case "nexustag": entityClass = NexusTag.class; break;
            default: return invalid("err.type.invalid");
        }

        // Look up the thing to see who owns it
        final DAO<? extends SocialEntity> dao = configuration.getDaoForEntityClass(entityClass);
        final SocialEntity entity = dao.findByUuid(uuid);

        final VoteSummary summary;
        switch (entity.getVisibility()) {
            case everyone:
                summary = voteSummaryDAO.findByUuid(uuid);
                break;
            case owner:
            case hidden:
            default:
                summary = account != null && entity.getOwner().equals(account.getUuid())
                        ? voteSummaryDAO.findByUuid(uuid)
                        : null;
        }

        return summary != null ? ok(summary) : notFound();
    }

}
