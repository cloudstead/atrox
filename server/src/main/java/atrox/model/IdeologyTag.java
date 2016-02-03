package atrox.model;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class IdeologyTag extends AccountOwnedEntity {

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String ideology;

    // Can be a WorldActor, WorldEvent or EventActor
    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String entity;

}
