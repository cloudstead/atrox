package atrox.model;

import lombok.NoArgsConstructor;

import javax.persistence.Entity;

@Entity @NoArgsConstructor
public class Ideology extends CanonicallyNamedEntity {

    public Ideology (String name) { super(name); }

}
