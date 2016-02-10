package atrox.model.tag;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity @Inheritance(strategy=InheritanceType.TABLE_PER_CLASS) @Accessors(chain=true)
public class IdeaTag extends GenericEntityTag {

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String ideology;

}
