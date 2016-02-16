package histori.model.canonical;

import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity @Inheritance(strategy= InheritanceType.TABLE_PER_CLASS) @NoArgsConstructor @Accessors(chain=true)
public class ImpactType extends CanonicalEntity {

    public ImpactType (String name) { super(name); }

}
