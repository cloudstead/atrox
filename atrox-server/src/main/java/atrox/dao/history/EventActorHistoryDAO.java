package atrox.dao.history;

import atrox.model.canonical.WorldActor;
import atrox.model.canonical.WorldEvent;
import atrox.model.history.EventActorHistory;
import org.springframework.stereotype.Repository;

@Repository public class EventActorHistoryDAO extends AssociatorEntityDAO<EventActorHistory, WorldEvent, WorldActor> {}
