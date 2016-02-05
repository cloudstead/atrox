package atrox.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class WorldActor extends CanonicallyNamedEntity {

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String parentActor;

}
