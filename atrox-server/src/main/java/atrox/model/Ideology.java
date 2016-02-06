package atrox.model;

import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Entity;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class Ideology extends CanonicallyNamedEntity {

    public Ideology (String name) { super(name); }

}
