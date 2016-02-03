package atrox.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.Column;
import javax.persistence.Entity;

import static org.cobbzilla.wizard.model.UniquelyNamedEntity.NAME_MAXLEN;

@Entity @Accessors(chain=true)
public class LoginAttempt extends IdentifiableBase {

    @Column(length=64, nullable=false, updatable=false)
    @Getter @Setter private String sourceIp;

    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String name;

    @Column(length=1000, nullable=false, updatable=false)
    @Getter @Setter private String notes;
}
