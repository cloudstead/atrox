package atrox.model.tags;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity @Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)  @Accessors(chain=true)
public class EventEffectTag extends EntityTag {

    public static final String[] UNIQUES = {"worldEvent", "effectType"};
    @Override @Transient @JsonIgnore public String[] getUniqueProperties() { return UNIQUES; }

    // references WorldEvent
    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String worldEvent;

    // references EffectType
    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String effectType;

    @Getter @Setter private long lowEstimate;
    @Getter @Setter private long midEstimate;
    @Getter @Setter private long highEstimate;

}
