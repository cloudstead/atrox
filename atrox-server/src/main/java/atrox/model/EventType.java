package atrox.model;

import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Entity;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class EventType extends CanonicallyNamedEntity {

    public EventType (String name) { super(name); }

}
