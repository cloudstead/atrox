package atrox.model.history;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity @Inheritance(strategy=InheritanceType.TABLE_PER_CLASS) @Accessors(chain=true)
public class EventActorHistory extends EntityHistory {

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String eventActor;

}
