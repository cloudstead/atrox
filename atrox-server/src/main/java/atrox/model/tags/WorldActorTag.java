package atrox.model.tags;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity @Accessors(chain=true)
public class WorldActorTag extends EntityTag {

    public static final String[] UNIQUES = {"worldActor"};
    @Override public String[] getUniqueProperties() { return UNIQUES; }

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String worldActor;

}
