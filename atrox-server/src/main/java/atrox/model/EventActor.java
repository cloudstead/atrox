package atrox.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity @Inheritance(strategy=InheritanceType.TABLE_PER_CLASS) @NoArgsConstructor @Accessors(chain=true)
public class EventActor extends CanonicallyNamedEntity {

    @Override public String getName() { return getUuid(); }
    @Override public CanonicallyNamedEntity setName(String val) { return this; } // noop

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String worldActor;

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String worldEvent;

}
