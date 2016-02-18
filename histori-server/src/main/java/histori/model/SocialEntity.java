package histori.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import histori.ApiConstants;
import histori.model.support.EntityCommentary;
import histori.model.support.EntityVisibility;
import histori.model.support.TimePoint;
import histori.model.support.VoteSummary;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ResultPage;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@MappedSuperclass @Accessors(chain=true)
public abstract class SocialEntity extends AccountOwnedEntity implements VersionedEntity {

    @Transient @JsonIgnore public String getSortField() { return "ctime"; }
    @Transient @JsonIgnore public ResultPage.SortOrder getSortOrder() { return ResultPage.SortOrder.DESC; }

    @Embedded @Getter @Setter private EntityCommentary commentary = new EntityCommentary();
    @Embedded @Getter @Setter private VoteSummary votes = new VoteSummary();

    @Transient @JsonIgnore public JavaType getSearchResultType() { return SearchResults.jsonType(getClass()); }

    @Column(nullable=false, length=20)
    @Enumerated(EnumType.STRING)
    @Getter @Setter private EntityVisibility visibility = EntityVisibility.everyone;

    @Getter @Setter private int version;

    public Map<String, String> getBounds(TimePoint start, TimePoint end) {
        final Map<String, String> bounds = new HashMap<>();
        bounds.put(ApiConstants.BOUND_RANGE, TimePoint.formatSearchRange(start, end));
        return bounds;
    }

}
