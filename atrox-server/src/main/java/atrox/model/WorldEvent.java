package atrox.model;

import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Entity;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class WorldEvent extends CanonicallyNamedEntity {

    public WorldEvent (String name) { super(name); }

}
