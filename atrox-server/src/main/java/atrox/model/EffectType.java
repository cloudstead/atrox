package atrox.model;

import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Entity;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class EffectType extends CanonicallyNamedEntity {

    public EffectType (String name) { super(name); }

}
