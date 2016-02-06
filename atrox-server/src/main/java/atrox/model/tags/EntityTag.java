package atrox.model.tags;

import atrox.model.AccountOwnedEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public abstract class EntityTag extends AccountOwnedEntity {

    @Override @Transient @JsonIgnore public String[] getAssociated() { return getUniqueProperties(); }

}
