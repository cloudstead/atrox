package histori.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.validation.constraints.Size;

import static histori.ApiConstants.NAME_MAXLEN;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.urlEncode;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@MappedSuperclass @NoArgsConstructor @Accessors(chain=true) @ToString(of="canonicalName")
public abstract class CanonicalEntity implements Identifiable {

    public static final String WORD_SEP = "-";

    public CanonicalEntity(String name) { setName(name); }

    @Override public void beforeCreate() { checkName(); }
    @Override public void beforeUpdate() { checkName(); }
    public void checkName() {
        if (empty(getName())) {
            throw invalidEx("err.name.required", "Name is required");
        }
    }

    @Id @Column(length=NAME_MAXLEN, unique=true, nullable=false, updatable=false)
    private String canonicalName;

    public String getCanonicalName() {
        if (canonicalName == null) canonicalName = canonicalize(getName());
        return canonicalName;
    }
    public void setCanonicalName(String val) { this.canonicalName = val; }

    public static String canonicalize(String val) {
        return val == null ? "" : urlEncode(val.trim().replace("_", WORD_SEP).replace(" ", WORD_SEP).toLowerCase());
    }

    @Override public String getUuid() { return getCanonicalName(); }
    @Override public void setUuid(String uuid) { setCanonicalName(uuid); }

    @Column(length=NAME_MAXLEN, unique=true, nullable=false)
    @HasValue(message="err.name.empty")
    @Size(min=1, max=NAME_MAXLEN, message="err.name.length")
    @Getter private String name;
    public CanonicalEntity setName (String val) {
        final String canonical = canonicalize(val);
        if (this.name != null && !canonicalize(this.name).equals(canonical)) throw invalidEx("err.canonicalName.cannotChange", "Name cannot be changed");
        this.name = val;
        this.canonicalName = canonical;
        return this;
    }

    @Column(length=NAME_MAXLEN)
    @Getter @Setter private String aliasFor;

}