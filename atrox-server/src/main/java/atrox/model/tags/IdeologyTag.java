package atrox.model.tags;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity @Inheritance(strategy=InheritanceType.TABLE_PER_CLASS) @Accessors(chain=true)
public class IdeologyTag extends EntityTag {

    public static final String[] UNIQUES = {"ideology", "entity"};
    @Override @Transient @JsonIgnore public String[] getUniqueProperties() { return UNIQUES; }

    public static final String[] TAGGABLE = {
            WorldEventTag.class.getSimpleName(),
            WorldActorTag.class.getSimpleName(),
            EventActorTag.class.getSimpleName(),
            IncidentTypeTag.class.getSimpleName(),
            EventIncidentTag.class.getSimpleName(),
            ImpactTypeTag.class.getSimpleName(),
            EventImpactTag.class.getSimpleName(),
            IdeologyTag.class.getSimpleName(),
            CitationTag.class.getSimpleName()
    };
    @Transient @JsonIgnore public String[] getTaggableEntities() { return TAGGABLE; }

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String ideology;

    // Can be just about anything
    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String entity;

}
