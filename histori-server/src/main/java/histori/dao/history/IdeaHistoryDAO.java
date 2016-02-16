package histori.dao.history;

import histori.model.history.IdeaHistory;
import org.springframework.stereotype.Repository;

@Repository public class IdeaHistoryDAO extends EntityHistoryDAO<IdeaHistory> {

    protected String propertyName(String entityType) { return "entity"; }

}