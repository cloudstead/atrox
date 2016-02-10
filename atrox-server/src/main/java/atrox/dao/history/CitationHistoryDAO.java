package atrox.dao.history;

import atrox.model.history.CitationHistory;
import org.springframework.stereotype.Repository;

@Repository public class CitationHistoryDAO extends EntityHistoryDAO<CitationHistory> {

    protected String propertyName(String entityType) { return "entity"; }

}
