package atrox.model.internal;

import com.github.jmkgreen.morphia.annotations.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Column;

@Entity
@NoArgsConstructor @Accessors(chain=true) @Slf4j
public class EntityPointer extends IdentifiableBase {

    @Column(length=100, nullable=false, updatable=false)
    @Getter @Setter private String entityType;

    public EntityPointer(String uuid, String className) {
        setUuid(uuid);
        setEntityType(className);
    }

}
