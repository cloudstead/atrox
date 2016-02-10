package atrox.dao.history;

import atrox.model.history.IdeaHistory;
import org.springframework.stereotype.Repository;

@Repository public class IdeaHistoryDAO extends EntityHistoryDAO<IdeaHistory> {

    protected String propertyName(String entityType) { return "entity"; }

}