package atrox.model;

import atrox.ApiConstants;
import atrox.model.support.EntityCommentary;
import atrox.model.support.EntityVisibility;
import atrox.model.support.TimePoint;
import atrox.model.support.VoteSummary;
import atrox.model.tags.EntityTag;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.model.StrongIdentifiableBase;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="entityType")
@MappedSuperclass @Accessors(chain=true) @NoArgsConstructor
public abstract class AccountOwnedEntity extends StrongIdentifiableBase {

    public static final String[] UNIQUES = new String[0];
    @Transient @JsonIgnore public String[] getUniqueProperties() { return UNIQUES; }
    @Transient @JsonIgnore public String getSortField() { return "ctime"; }
    @Transient @JsonIgnore public ResultPage.SortOrder getSortOrder() { return ResultPage.SortOrder.DESC; }

    public static final String[] ASSOCIATED = new String[0];
    @Transient @JsonIgnore public String[] getAssociated() { return ASSOCIATED; }
    @Transient public boolean hasAssociated() { return !empty(getAssociated()); }

    public static final String[] BASIC_TAG_TYPES = {"CitationTag", "IdeologyTag"};
    @Transient @JsonIgnore @Getter(lazy=true) private final String[] tagTypes = initTagTypes();
    public String[] initTagTypes() { return ArrayUtil.append(BASIC_TAG_TYPES, getClass().getSimpleName()+"Tag"); }

    public AccountOwnedEntity(String owner) { setOwner(owner); }

    @Column(nullable = false, length = UUID_MAXLEN) @Getter @Setter private String owner;
    public boolean hasOwner() { return !empty(owner); }
    public boolean isOwner(String uuid) { return hasOwner() && getOwner().equals(uuid); }

    @Getter @Setter private int entityVersion;

    @Embedded @Getter @Setter private EntityCommentary commentary = new EntityCommentary();
    @Embedded @Getter @Setter private VoteSummary votes = new VoteSummary();

    @Transient @JsonIgnore public JavaType getSearchResultType() { return SearchResults.jsonType(getClass()); }

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Getter @Setter private EntityVisibility visibility = EntityVisibility.owner;

    public Map<String, String> getBounds(TimePoint start, TimePoint end) {
        final Map<String, String> bounds = new HashMap<>();
        bounds.put(ApiConstants.BOUND_RANGE, TimePoint.formatSearchRange(start, end));
        return bounds;
    }

    // Used to populate json
    @Transient @Getter @Setter
    public Map<String, AccountOwnedEntity> associations = new HashMap<>();

    public AccountOwnedEntity getAssociation(String type) { return associations.get(type); }

    public AccountOwnedEntity getAssociation(Class<? extends AccountOwnedEntity> type) {
        return associations.get(type.getSimpleName());
    }

    public AccountOwnedEntity addAssociation(AccountOwnedEntity toAdd) {
        associations.put(toAdd.getClass().getSimpleName(), toAdd);
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
