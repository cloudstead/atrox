package atrox.model;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class EventTypeTag extends AccountOwnedEntity {

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String eventType;

    // References a WorldEvent
    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String worldEvent;

}
