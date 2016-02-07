package atrox.model;

import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity @Inheritance(strategy=InheritanceType.TABLE_PER_CLASS) @NoArgsConstructor @Accessors(chain=true)
public class IncidentType extends CanonicallyNamedEntity {

    public IncidentType(String name) { super(name); }

}
