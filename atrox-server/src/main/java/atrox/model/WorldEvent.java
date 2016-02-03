package atrox.model;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.IdentifiableBase;
import org.hibernate.annotations.Type;
import org.joda.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;

@Entity
public class WorldEvent extends CanonicallyNamedEntity {

    @Column(nullable=false)
    @Type(type="org.jadira.usertype.dateandtime.joda.PersistentLocalDateTime")
    @Getter @Setter private LocalDateTime start;

    @Column(nullable=false)
    @Type(type="org.jadira.usertype.dateandtime.joda.PersistentLocalDateTime")
    @Getter @Setter private LocalDateTime end;

    @Getter @Setter private long lowEstimate;
    @Getter @Setter private long midEstimate;
    @Getter @Setter private long highEstimate;

}
