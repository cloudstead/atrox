package histori.dao.history;

import histori.model.history.CitationHistory;
import org.springframework.stereotype.Repository;

@Repository public class CitationHistoryDAO extends EntityHistoryDAO<CitationHistory> {

    protected String propertyName(String entityType) { return "entity"; }

}
