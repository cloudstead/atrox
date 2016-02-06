package atrox.model;

import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity @NoArgsConstructor
@Accessors(chain=true)
public class WorldActor extends CanonicallyNamedEntity {

    public WorldActor (String name) { super(name); }

}
