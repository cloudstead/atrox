package atrox.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.ResultPage;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.model.UniquelyNamedEntity.NAME_MAXLEN;

/**
 * A CanonicallyNamedEntity has a name, which must be unique, and a canonical name, which must also be unique.
 * The canonical name simplifies the inbound name, in the interest of avoiding entities whose names differ only in
 * terms of
 */
@MappedSuperclass @NoArgsConstructor @Accessors(chain=true)
public abstract class CanonicallyNamedEntity extends AccountOwnedEntity {

    public CanonicallyNamedEntity (String name) { setName(name); }

    public static final String[] UNIQUES = {"canonicalName"};
    @Override public String[] getUniqueProperties() { return UNIQUES; }
    @Override public String getSortField() { return UNIQUES[0]; }
    @Override public ResultPage.SortOrder getSortOrder() { return ResultPage.SortOrder.ASC; }

    @Column(length=NAME_MAXLEN, unique=true, nullable=false, updatable=false)
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Getter private String name;
    public CanonicallyNamedEntity setName (String val) { canonicalName = canonicalize(val); return this; }

    public static String canonicalize(String val) {
        return empty(val) ? "" : val.replaceAll("\\W+", "_").toLowerCase();
    }

    @Column(length=NAME_MAXLEN, unique=true, nullable=false, updatable=false)
    @Getter @Setter private String canonicalName;

}
