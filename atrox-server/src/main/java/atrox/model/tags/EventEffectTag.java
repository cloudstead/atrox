package atrox.model.tags;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity @Accessors(chain=true)
public class EventEffectTag extends EntityTag {

    public static final String[] UNIQUES = {"worldEvent", "effectType"};
    @Override public String[] getUniqueProperties() { return UNIQUES; }

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
