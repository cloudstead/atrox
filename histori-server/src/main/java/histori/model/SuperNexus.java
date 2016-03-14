package histori.model;

import histori.model.cache.VoteSummary;
import histori.model.support.EntityVisibility;
import histori.model.support.GeoBounds;
import histori.model.support.TimeRange;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.*;
import javax.validation.constraints.Size;

import static histori.ApiConstants.NAME_MAXLEN;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Entity @NoArgsConstructor @Accessors(chain=true)
public class SuperNexus extends IdentifiableBase {

    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Getter @Setter private String canonicalName;

    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Getter private String name;
    public boolean hasName() { return !empty(name); }

    public void setName(String name) {
        this.name = name;
        this.canonicalName = CanonicalEntity.canonicalize(name);
    }

    @Embedded @Getter @Setter private TimeRange timeRange;
    @Embedded @Getter @Setter private GeoBounds bounds;
    @Embedded @Getter @Setter private VoteSummary votes;

    @Column(nullable=false, length=20)
    @Enumerated(EnumType.STRING)
    @Getter @Setter private EntityVisibility visibility = EntityVisibility.everyone;

    // set to true in SuperNexusDAO when a Nexus has been deleted (we don't know how to shrink the geo/time)
    // todo: a background job refreshes the SuperNexus geo/time bounds
    @Getter @Setter private boolean dirty = false;
    public SuperNexus setDirty () { this.dirty = true; return this; }

    // only present for visibility levels other than 'everyone'
    @Column(length=UUID_MAXLEN)
    @Getter @Setter private String account;
    public boolean hasAccount() { return account != null; }
    public boolean isAccount(String uuid)     { return    uuid != null && hasAccount() && getAccount().equals(uuid); }
    public boolean isAccount(Account account) { return account != null && hasAccount() && account.getUuid().equals(getAccount()); }

    public SuperNexus(Nexus nexus) {
        setName(nexus.getName());
        this.timeRange = nexus.getTimeRange();
        this.bounds = nexus.getBounds();
        this.visibility = nexus.getVisibility();
        this.account = nexus.getVisibility() == EntityVisibility.everyone ? null : nexus.getOwner();
    }

    public boolean update(Nexus nexus) {
        boolean changed = false;
        if (nexus.getTimeRange().isOutside(getTimeRange())) {
            timeRange = nexus.getTimeRange();
            changed = true;
        }
        if (nexus.getBounds().isOutside(getBounds())) {
            bounds = nexus.getBounds();
            changed = true;
        }
        return changed;
    }
}
