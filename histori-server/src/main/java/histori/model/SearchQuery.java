package histori.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.support.EntityVisibility;
import histori.model.support.GeoBounds;
import histori.model.support.SearchSortOrder;
import histori.model.support.TimeRange;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.model.IdentifiableBase;

import javax.persistence.*;
import java.util.Comparator;
import java.util.List;

import static javax.persistence.EnumType.STRING;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@Entity @NoArgsConstructor @Accessors(chain=true)
@EqualsAndHashCode(of={"query", "visibility", "timeRange", "bounds", "preferredOwners", "blockedOwners"}, callSuper=false)
public class SearchQuery extends IdentifiableBase {

    public SearchQuery (SearchQuery other) { copy(this, other); setUuid(null); }

    @Getter @Setter private boolean useCache = true;

    @Getter @Setter private Long timeout;
    public boolean hasTimeout () { return timeout != null; }

    @Column(length=1024, updatable=false)
    @Getter @Setter private String query;

    @Embedded @Getter @Setter private TimeRange timeRange;
    public SearchQuery setRange(String from, String to) {
        try {
            return setTimeRange(new TimeRange(from, to));
        } catch (Exception e) {
            throw invalidEx("err.timeRange.invalid", e.getMessage());
        }
    }

    @Embedded @Getter @Setter private GeoBounds bounds;
    public SearchQuery setBounds(double north, double south, double east, double west) {
        return setBounds(new GeoBounds(north, south, east, west));
    }

    @Enumerated(value=STRING)
    @Getter @Setter private EntityVisibility visibility;
    public boolean hasVisibility () { return visibility != null; }

    @Enumerated(value=STRING)
    @Getter @Setter private SearchSortOrder summarySortOrder = SearchSortOrder.vote_tally;

    @Enumerated(value=STRING)
    @Getter @Setter private SearchSortOrder nexusSortOrder = SearchSortOrder.vote_tally;

    @JsonIgnore private String preferredOwners = null;
    public String getPreferredOwners() {
        if (!empty(preferredOwners)) return preferredOwners;
        return preferredOwnersList != null ? StringUtil.toString(preferredOwnersList, ",") : null;
    }

    public void setPreferredOwners(String preferredOwners) {
        this.preferredOwners = preferredOwners;
        this.preferredOwnersList = StringUtil.split(preferredOwners, ", ");
    }

    public boolean hasPreferredOwners () { return !empty(getPreferredOwners()); }

    @Transient private List<String> preferredOwnersList = null;

    public List<String> getPreferredOwnersList() {
        if (preferredOwnersList == null && preferredOwners != null) {
            preferredOwnersList = StringUtil.split(preferredOwners, ", ");
        }
        return preferredOwnersList;
    }

    @JsonIgnore private String blockedOwners = null;
    public String getBlockedOwners() {
        if (!empty(blockedOwners)) return blockedOwners;
        return blockedOwnersList != null ? StringUtil.toString(blockedOwnersList, ",") : null;
    }

    public void setBlockedOwners(String blockedOwners) {
        this.blockedOwners = blockedOwners;
        this.blockedOwnersList = StringUtil.split(blockedOwners, ", ");
    }

    public boolean hasBlockedOwners () { return !empty(getBlockedOwners()); }

    @Transient private List<String> blockedOwnersList = null;

    public List<String> getBlockedOwnersList() {
        if (blockedOwnersList == null && blockedOwners != null) {
            blockedOwnersList = StringUtil.split(blockedOwners, ", ");
        }
        return blockedOwnersList;
    }

    public boolean hasBlockedOwner(String owner) { return hasBlockedOwners() && getBlockedOwnersList().contains(owner); }

    // How to sort different nexuses that have the same name
    @Transient @JsonIgnore public Comparator<NexusView> getNexusComparator() { return new NexusComparator(); }

    private class NexusComparator implements Comparator<NexusView> {
        @Override public int compare(NexusView n1, NexusView n2) {
            // give preferred owners special treatment first
            if (hasPreferredOwners()) {
                final List<String> preferred = getPreferredOwnersList();
                int n1index = n1.hasOwner() ? preferred.indexOf(n1.getOwner()) : Integer.MAX_VALUE;
                int n2index = n2.hasOwner() ? preferred.indexOf(n2.getOwner()) : Integer.MAX_VALUE;
                if (n1index != n2index) return (n2index - n1index);
            }
            // otherwise, newest first
            return nexusSortOrder == null
                    ? Nexus.comparator(SearchSortOrder.newest).compare(n1, n2)
                    : Nexus.comparator(nexusSortOrder).compare(n1, n2);
        }
    }
}

