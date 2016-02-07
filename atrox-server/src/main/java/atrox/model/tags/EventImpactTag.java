package atrox.model.tags;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity @Inheritance(strategy=InheritanceType.TABLE_PER_CLASS) @Accessors(chain=true)
public class EventImpactTag extends EntityTag {

    public static final String[] UNIQUES = {"eventImpact"};
    @Override @Transient @JsonIgnore public String[] getUniqueProperties() { return UNIQUES; }

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String eventImpact;

    @Getter @Setter private long lowEstimate;
    @Getter @Setter private long midEstimate;
    @Getter @Setter private long highEstimate;

}
