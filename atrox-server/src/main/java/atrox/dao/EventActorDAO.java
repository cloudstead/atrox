package atrox.dao;

import atrox.model.tags.EventActorTag;
import atrox.model.WorldActor;
import atrox.model.WorldEvent;
import org.springframework.stereotype.Repository;

@Repository public class EventActorDAO extends AssociatorEntityDAO<EventActorTag, WorldEvent, WorldActor> {}
