package atrox.model.tags;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity @Inheritance(strategy=InheritanceType.TABLE_PER_CLASS) @Accessors(chain=true)
public class ImpactTypeTag extends EntityTag {

    public static final String[] UNIQUES = {"impactType"};
    @Override @Transient @JsonIgnore public String[] getUniqueProperties() { return UNIQUES; }

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String impactType;

}
