package atrox.dao.tags;

import atrox.dao.AssociatorEntityDAO;
import atrox.model.IncidentType;
import atrox.model.tags.EventImpactTag;
import atrox.model.WorldEvent;
import org.springframework.stereotype.Repository;

@Repository public class EventImpactTagDAO extends AssociatorEntityDAO<EventImpactTag, WorldEvent, IncidentType> {}
