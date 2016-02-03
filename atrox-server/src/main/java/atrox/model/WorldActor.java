package atrox.model;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.Size;

@Entity
public class WorldActor extends CanonicallyNamedEntity {

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String parentActor;

}
