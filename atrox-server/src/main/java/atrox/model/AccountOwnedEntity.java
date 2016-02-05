package atrox.model;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.StrongIdentifiableBase;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AccountOwnedEntity extends StrongIdentifiableBase {

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String owner;

}
