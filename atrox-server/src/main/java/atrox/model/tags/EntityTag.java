package atrox.model.tags;

import atrox.model.AccountOwnedEntity;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public abstract class EntityTag extends AccountOwnedEntity {

    @Transient public String[] getAssociated() { return getUniqueProperties(); }

}
