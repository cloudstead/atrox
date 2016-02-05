package atrox.model;

import lombok.NoArgsConstructor;

import javax.persistence.Entity;

@Entity @NoArgsConstructor
public class EventType extends CanonicallyNamedEntity {

    public EventType (String name) { super(name); }

}
