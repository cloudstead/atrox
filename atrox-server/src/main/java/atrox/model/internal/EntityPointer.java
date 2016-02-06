package atrox.model.internal;

import atrox.model.AccountOwnedEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity @NoArgsConstructor @Accessors(chain=true) @Slf4j
public class EntityPointer extends AccountOwnedEntity {

    public static final int ENTITY_TYPE_MAXLEN = 100;

    @Column(length=ENTITY_TYPE_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String entityType;

    // Allow creation with a UUID already set
    @Override public void beforeCreate() {}

    public EntityPointer(String uuid, String className) {
        setUuid(uuid);
        setEntityType(className);
        setOwner("_system_");
    }

}
