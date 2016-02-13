package atrox.model.canonical;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity @Inheritance(strategy=InheritanceType.TABLE_PER_CLASS) @NoArgsConstructor @Accessors(chain=true)
public class EventIncident extends CanonicalEntity {

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String worldEvent;

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String incidentType;

}
