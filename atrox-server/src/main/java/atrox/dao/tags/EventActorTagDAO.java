package atrox.dao.tags;

import atrox.dao.AssociatorEntityDAO;
import atrox.model.WorldActor;
import atrox.model.WorldEvent;
import atrox.model.tags.EventActorTag;
import org.springframework.stereotype.Repository;

@Repository public class EventActorTagDAO extends AssociatorEntityDAO<EventActorTag, WorldEvent, WorldActor> {}
