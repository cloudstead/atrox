package atrox.model.tag;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity @Inheritance(strategy=InheritanceType.TABLE_PER_CLASS) @Accessors(chain=true)
public class CitationTag extends GenericEntityTag {

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String citation;

    @Column(length=200)
    @Getter @Setter private String citationDetails;

}
