package histori.model;

import histori.ApiConstants;
import histori.model.support.EntityCommentary;
import histori.model.support.EntityVisibility;
import histori.model.support.TimePoint;
import histori.model.support.VoteSummary;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.model.StrongIdentifiableBase;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@MappedSuperclass @Accessors(chain=true)
public abstract class SocialEntity extends AccountOwnedEntity {

    @Transient @JsonIgnore public String getSortField() { return "ctime"; }
    @Transient @JsonIgnore public ResultPage.SortOrder getSortOrder() { return ResultPage.SortOrder.DESC; }

    @Transient @JsonIgnore public abstract String[] getAssociated();

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
    public Map<String, StrongIdentifiableBase> associations = new HashMap<>();

    public StrongIdentifiableBase getAssociation(String type) { return associations.get(type); }

    public StrongIdentifiableBase getAssociation(Class<? extends SocialEntity> type) {
        return associations.get(type.getSimpleName());
    }

    public SocialEntity addAssociation(StrongIdentifiableBase toAdd) {
        associations.put(toAdd.simpleName(), toAdd);
        return this;
    }
}
