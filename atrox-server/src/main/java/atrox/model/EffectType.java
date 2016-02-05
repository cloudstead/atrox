package atrox.model;

import lombok.NoArgsConstructor;

import javax.persistence.Entity;

@Entity @NoArgsConstructor
public class EffectType extends CanonicallyNamedEntity {

    public EffectType (String name) { super(name); }

}
