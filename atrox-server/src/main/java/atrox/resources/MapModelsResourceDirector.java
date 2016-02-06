package atrox.resources;

import atrox.server.AtroxConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;

import static atrox.ApiConstants.MAP_ENTITIES_ENDPOINT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(MAP_ENTITIES_ENDPOINT)
@Service @Slf4j
public class MapModelsResourceDirector {

    @Autowired private AtroxConfiguration configuration;

    @Path("/{entityType}")
    public MapModelsResource getEntityResource (@PathParam("entityType") String entityType) {
        return MapModelsResource.getResource(entityType, configuration);
    }

//
//    @POST
//    @Path(ApiConstants.EP_EVENTS_BY_NAME+"/{name}")
//    public Response addEvent (@Context HttpContext ctx,
//                              WorldEventView incoming) {
//
//        final Account account = userPrincipal(ctx);
//        final String accountUuid = account.getUuid();
//
//        // Does an event with this name already exist?
//        final WorldEvent worldEvent = incoming.getWorldEvent();
//        worldEvent.setOwner(accountUuid);
//
//        final WorldEventView outgoing = new WorldEventView();
//
//        final WorldEvent foundEvent = worldEventDAO.findOrCreateByCanonicalName(worldEvent);
//        outgoing.setWorldEvent(foundEvent);
//
//        final WorldEventTag eventDetails = worldEventDetailsDAO.findByEventAndOwner(foundEvent, account);
//        if (eventDetails == null) {
//            final WorldEventTag details = incoming.getWorldEventTag();
//            details.setOwner(accountUuid);
//            details.setWorldEvent(foundEvent.getUuid());
//            worldEventDetailsDAO.create(details);
//        }
//
//        for (WorldActor actor : incoming.getActors()) {
//            actor.setOwner(accountUuid);
//            final WorldActor foundActor = worldActorDAO.findOrCreateByCanonicalName(actor);
//            outgoing.addActor(foundActor);
//            final EventActorTag eventActorTag = eventActorDAO.createOrUpdateByAssociation(account, actor.getEventActor(), foundEvent, foundActor);
//        }
//
//        for (EventEffectTag effect : incoming.getEffects()) {
//
//            EffectType foundEffectType = effectTypeDAO.findOrCreateByCanonicalName(account, effect.getEffectType());
//            final EventEffectTag foundEffect = eventEffectDAO.findByAssociation(account, foundEvent, foundEffectType);
//            outgoing.addEffect(foundEffect);
//        }
//        return ok(outgoing);
//    }
}
