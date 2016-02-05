package atrox.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.model.UniquelyNamedEntity.NAME_MAXLEN;

@MappedSuperclass
public abstract class CanonicallyNamedEntity extends AccountOwnedEntity {

    @Column(length=NAME_MAXLEN, unique=true, nullable=false, updatable=false)
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Getter private String name;
    public void setName (String val) { canonicalName = canonicalize(val); }

    public static String canonicalize(String val) {
        return empty(val) ? "" : val.replaceAll("\\W", "_").toLowerCase();
    }

    @Column(length=NAME_MAXLEN, unique=true, nullable=false, updatable=false)
    @Getter @Setter private String canonicalName;

}
