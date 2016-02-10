package atrox.dao.history;

import atrox.model.canonical.IncidentType;
import atrox.model.history.EventImpactHistory;
import atrox.model.canonical.WorldEvent;
import org.springframework.stereotype.Repository;

@Repository public class EventImpactHistoryDAO extends AssociatorEntityDAO<EventImpactHistory, WorldEvent, IncidentType> {}
