package histori.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.*;
import javax.validation.constraints.Size;

import static histori.ApiConstants.NAME_MAXLEN;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@MappedSuperclass @NoArgsConstructor @Accessors(chain=true) @ToString(of="canonicalName")
public abstract class CanonicalEntity implements VersionedEntity {

    public CanonicalEntity(String name) { setName(name); }

    @Override public void beforeCreate() { if (empty(getName())) throw invalidEx("err.name.required"); }

    @Id @Column(length=NAME_MAXLEN, unique=true, nullable=false, updatable=false)
    private String canonicalName;

    public String getCanonicalName() {
        if (canonicalName == null) canonicalName = canonicalize(getName());
        return canonicalName;
    }
    public void setCanonicalName(String val) { this.canonicalName = canonicalize(val); }

    public static String canonicalize(String val) {
        return empty(val) ? "" : val.replaceAll("\\W+", "_").toLowerCase();
    }

    @Override public String getUuid() { return getCanonicalName(); }
    @Override public void setUuid(String uuid) { setCanonicalName(uuid); }

    @Column(length=NAME_MAXLEN, unique=true, nullable=false)
    @HasValue(message="err.name.empty")
    @Size(min=2, max=NAME_MAXLEN, message="err.name.tooLong")
    @Getter private String name;
    public CanonicalEntity setName (String val) {
        final String canonical = canonicalize(val);
        if (this.name != null && !canonicalize(this.name).equals(canonical)) throw invalidEx("err.canonicalName.cannotChange");
        this.name = val;
        this.canonicalName = canonical;
        return this;
    }

    @Column(length=NAME_MAXLEN)
    @Getter @Setter private String aliasFor;

    @Getter @Setter private int version;

    private static final String[] ID_FIELDS = {"canonicalName"};
    @Override public String[] getIdentifiers() { return new String[] { getCanonicalName() }; }
    @Override public String[] getIdentifierFields() { return ID_FIELDS; }

}