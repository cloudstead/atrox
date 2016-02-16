package histori.model.canonical;

import histori.model.TaggableEntity;
import histori.model.history.EntityHistory;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

/**
 * A CanonicalEntity has a name, which must be unique, and a canonical name, which must also be unique.
 * The canonical name simplifies the inbound name, in the interest of avoiding entities whose names differ only in
 * terms of
 */
@MappedSuperclass @NoArgsConstructor @Accessors(chain=true)
public abstract class CanonicalEntity extends TaggableEntity {

    public static final int NAME_MAXLEN = 200;

    public CanonicalEntity(String name) { setName(name); }

    @Column(length=NAME_MAXLEN, unique=true, nullable=false, updatable=false)
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Getter private String name;
    public CanonicalEntity setName (String val) { this.name = val; canonicalName = canonicalize(val); return this; }

    public static String canonicalize(String val) {
        return empty(val) ? "" : val.replaceAll("\\W+", "_").toLowerCase();
    }

    @Column(length=NAME_MAXLEN, unique=true, nullable=false, updatable=false)
    @Getter @Setter private String canonicalName;

    @Transient @Getter private List<EntityHistory> histories;
    public void addHistory(EntityHistory history) {
        if (histories == null) histories = new ArrayList<>();
        histories.add(history);
    }
}
