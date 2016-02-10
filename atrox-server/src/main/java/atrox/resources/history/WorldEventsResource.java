package atrox.resources.history;

import atrox.dao.history.EventActorHistoryDAO;
import atrox.dao.history.WorldActorHistoryDAO;
import atrox.model.canonical.WorldEvent;
import atrox.model.history.WorldActorHistory;
import atrox.model.history.WorldEventHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Service @Slf4j
public class WorldEventsResource extends HistoriesResource<WorldEvent, WorldEventHistory> {

    @Autowired private WorldActorHistoryDAO worldActorHistoryDAO;
    @Autowired private EventActorHistoryDAO eventActorHistoryDAO;

    protected void updateRelationships(WorldEventHistory history, WorldEventHistory updates) {

        if (updates.hasActors()) {
            for (WorldActorHistory actor : updates.getActors()) {
                actor = worldActorHistoryDAO.populateAssociated(actor);

            }
        }

        // update tags: ideas, citations
        super.updateRelationships(history, updates);
    }

}
