package histori.dao.history;

import histori.model.canonical.IncidentType;
import histori.model.history.EventImpactHistory;
import histori.model.canonical.WorldEvent;
import org.springframework.stereotype.Repository;

@Repository public class EventImpactHistoryDAO extends AssociatorEntityDAO<EventImpactHistory, WorldEvent, IncidentType> {}
