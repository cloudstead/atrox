package atrox.model.internal;

import atrox.model.canonical.CanonicalEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.model.StrongIdentifiableBase;

import javax.persistence.Column;
import javax.persistence.Entity;

import static atrox.ApiConstants.ENTITY_TYPE_MAXLEN;
import static atrox.model.canonical.CanonicalEntity.NAME_MAXLEN;

@Entity @NoArgsConstructor @Accessors(chain=true) @Slf4j
public class EntityPointer extends StrongIdentifiableBase {

    @Column(length=ENTITY_TYPE_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String entityType;

    @Column(length=NAME_MAXLEN, unique=true, updatable=false)
    @Getter @Setter private String name;

    @Column(length=NAME_MAXLEN, unique=true, updatable=false)
    @Getter @Setter private String canonicalName;

    public EntityPointer(StrongIdentifiableBase entity) {
        setUuid(entity.getUuid());
        setEntityType(entity.simpleName());
        if (entity instanceof CanonicalEntity) {
            final CanonicalEntity canonical = (CanonicalEntity) entity;
            setName(canonical.getName());
            setCanonicalName(canonical.getCanonicalName());
        }
    }

    // Allow creation with a UUID already set
    @Override public void beforeCreate() {}

}
