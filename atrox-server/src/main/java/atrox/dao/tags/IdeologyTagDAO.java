package atrox.dao.tags;

import atrox.model.tags.IdeologyTag;
import org.springframework.stereotype.Repository;

@Repository public class IdeologyTagDAO extends TagDAO<IdeologyTag> {

    protected String propertyName(String entityType) { return "entity"; }

}