package atrox.model.tags;

import atrox.model.AccountOwnedEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

@MappedSuperclass
public abstract class EntityTag extends AccountOwnedEntity {

    @Override @Transient @JsonIgnore public String[] getAssociated() { return getUniqueProperties(); }

    @Transient @JsonIgnore @Getter(lazy=true) private final String[] taggableEntities = initTaggable();
    protected String[] initTaggable() { return new String[] { getUniqueProperties()[0]+"Tag" }; }

    @Transient @JsonIgnore @Override public String[] getTagTypes () { return BASIC_TAG_TYPES; }

}
