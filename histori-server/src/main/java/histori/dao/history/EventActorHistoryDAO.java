package histori.dao.history;

import histori.model.canonical.WorldActor;
import histori.model.canonical.WorldEvent;
import histori.model.history.EventActorHistory;
import org.springframework.stereotype.Repository;

@Repository public class EventActorHistoryDAO extends AssociatorEntityDAO<EventActorHistory, WorldEvent, WorldActor> {}
