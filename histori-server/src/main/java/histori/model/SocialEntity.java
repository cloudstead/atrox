package histori.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.ApiConstants;
import histori.model.cache.VoteSummary;
import histori.model.support.EntityVisibility;
import histori.model.support.TimePoint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.ResultPage;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@MappedSuperclass @Accessors(chain=true)
public abstract class SocialEntity extends AccountOwnedEntity implements VersionedEntity {

    @Transient @JsonIgnore public String getSortField() { return "ctime"; }
    @Transient @JsonIgnore public ResultPage.SortOrder getSortOrder() { return ResultPage.SortOrder.DESC; }

    @Column(length=UUID_MAXLEN, updatable=false)
    @Getter @Setter private String origin;

    public static final int MARKDOWN_MAXLEN = 100000;
    @Size(max=MARKDOWN_MAXLEN, message="err.markdown.tooLong")
    @Column(length=MARKDOWN_MAXLEN)
    @Getter @Setter private String markdown;
    public boolean hasMarkdown () { return !empty(markdown); }

    @Column(nullable=false, length=20)
    @Enumerated(EnumType.STRING)
    @Getter @Setter private EntityVisibility visibility = EntityVisibility.everyone;

    public boolean isVisibleTo(Account account) { return visibility.isVisibleTo(this, account); }

    @Getter @Setter private int version;

    @Transient @Getter @Setter private VoteSummary votes;
    public boolean hasVotes() { return votes != null; }

    public Map<String, String> getBounds(TimePoint start, TimePoint end) {
        final Map<String, String> bounds = new HashMap<>();
        bounds.put(ApiConstants.BOUND_RANGE, TimePoint.formatSearchRange(start, end));
        return bounds;
    }

}
