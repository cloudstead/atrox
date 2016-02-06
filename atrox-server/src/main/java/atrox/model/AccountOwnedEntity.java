package atrox.model;

import atrox.ApiConstants;
import atrox.model.support.EntityCommentary;
import atrox.model.support.EntityVisibility;
import atrox.model.support.TimePoint;
import atrox.model.support.VoteSummary;
import atrox.model.tags.EntityTag;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.model.SemanticVersion;
import org.cobbzilla.wizard.model.StrongIdentifiableBase;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@MappedSuperclass @Accessors(chain=true) @NoArgsConstructor
public abstract class AccountOwnedEntity extends StrongIdentifiableBase {

    public static final String[] UNIQUES = new String[0];
    @Transient @JsonIgnore public String[] getUniqueProperties() { return UNIQUES; }
    @Transient @JsonIgnore public String getSortField() { return "ctime"; }
    @Transient @JsonIgnore public ResultPage.SortOrder getSortOrder() { return ResultPage.SortOrder.DESC; }

    public static final String[] ASSOCIATED = new String[0];
    @Transient @JsonIgnore public String[] getAssociated() { return ASSOCIATED; }
    @Transient public boolean hasAssociated() { return !empty(getAssociated()); }

    public AccountOwnedEntity (String owner) { setOwner(owner); }

    @Column(nullable=false, length=UUID_MAXLEN)
    @Getter @Setter private String owner;
    public boolean hasOwner() { return !empty(owner); }
    public boolean isOwner(String uuid) { return hasOwner() && getOwner().equals(uuid); }

    @Embedded @Getter @Setter private SemanticVersion version = new SemanticVersion();
    @Embedded @Getter @Setter private EntityCommentary commentary = new EntityCommentary();
    @Embedded @Getter @Setter private VoteSummary votes = new VoteSummary();

    @Transient @JsonIgnore public JavaType getSearchResultType() { return SearchResults.jsonType(getClass()); }

    public String incrementVersion () {
        version = SemanticVersion.incrementPatch(version);
        return version.toString();
    }

    @Column(nullable=false, length=20)
    @Enumerated(EnumType.STRING)
    @Getter @Setter private EntityVisibility visibility = EntityVisibility.owner;

    public Map<String, String> getBounds(TimePoint start, TimePoint end) {
        final Map<String, String> bounds = new HashMap<>();
        bounds.put(ApiConstants.BOUND_RANGE, TimePoint.formatSearchRange(start, end));
        return bounds;
    }

    // Used to populate json
    @Transient @Getter @Setter public Map<String, List<AccountOwnedEntity>> associations = new HashMap<>();

    public List<AccountOwnedEntity> getAssociationList(String associationEntityType) {
        List<AccountOwnedEntity> list = associations.get(associationEntityType);
        if (list == null) {
            list = new ArrayList<>();
            associations.put(associationEntityType, list);
        }
        return list;
    }

    public AccountOwnedEntity addAssociation(AccountOwnedEntity toAdd) {
        if (empty(toAdd)) return this;
        final String associationEntityType = toAdd.getClass().getSimpleName();
        List<AccountOwnedEntity> list = getAssociationList(associationEntityType);
        list.add(toAdd);
        return this;
    }

    public AccountOwnedEntity addAssociations(List<AccountOwnedEntity> toAdd) {
        if (empty(toAdd)) return this;
        final String associationEntityType = toAdd.get(0).getClass().getSimpleName();
        List<AccountOwnedEntity> list = getAssociationList(associationEntityType);
        list.addAll(toAdd);
        return this;
    }

    // Used to populate json
    @Transient @Getter @Setter public Map<String, List<EntityTag>> tags = new HashMap<>();

    public List<EntityTag> getTagList(String tagType) {
        List<EntityTag> list = tags.get(tagType);
        if (list == null) {
            list = new ArrayList<>();
            tags.put(tagType, list);
        }
        return list;
    }

    public AccountOwnedEntity addTag(EntityTag toAdd) {
        if (empty(toAdd)) return this;
        final String tagType = toAdd.getClass().getSimpleName();
        List<EntityTag> list = getTagList(tagType);
        list.add(toAdd);
        return this;
    }

    public AccountOwnedEntity addTags(List<EntityTag> toAdd) {
        if (empty(toAdd)) return this;
        final String associationEntityType = toAdd.get(0).getClass().getSimpleName();
        List<EntityTag> list = getTagList(associationEntityType);
        list.addAll(toAdd);
        return this;
    }

}
