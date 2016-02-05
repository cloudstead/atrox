package atrox.dao.tags;

import atrox.dao.AccountOwnedEntityDAO;
import atrox.model.Account;
import atrox.model.AccountOwnedEntity;
import atrox.model.tags.EntityTag;

import java.util.List;

public abstract class TagDAO<E extends AccountOwnedEntity> extends AccountOwnedEntityDAO<E> {

    public List<EntityTag> findTopTags(Account account) {
        return null;
    }

}
