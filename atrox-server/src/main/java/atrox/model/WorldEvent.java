package atrox.model;

import lombok.NoArgsConstructor;

import javax.persistence.Entity;

@Entity @NoArgsConstructor
public class WorldEvent extends CanonicallyNamedEntity {

    public WorldEvent (String name) { super(name); }

}
