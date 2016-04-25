package histori.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.ApiConstants;
import histori.model.cache.VoteSummary;
import histori.model.support.EntityVisibility;
import histori.model.support.TimePoint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.sql.SQLFieldTransformer;
import org.cobbzilla.wizard.dao.sql.SQLMappable;
import org.cobbzilla.wizard.model.ResultPage;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@MappedSuperclass @Accessors(chain=true)
public abstract class SocialEntity extends AccountOwnedEntity implements VersionedEntity, SQLMappable {

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

    @Getter @Setter private VoteSummary votes;
    public boolean hasVotes() { return votes != null; }

    @JsonIgnore public Map<String, String> getBounds(TimePoint start, TimePoint end) {
        final Map<String, String> bounds = new HashMap<>();
        bounds.put(ApiConstants.BOUND_RANGE, TimePoint.formatSearchRange(start, end));
        return bounds;
    }


    @Override @JsonIgnore public Map<String, SQLFieldTransformer> getSQLFieldTransformers() {
        final Map<String, SQLFieldTransformer> map = new HashMap<>();
        map.put("visibility", EntityVisibility.TRANSFORMER);
        map.put("up_votes", UPVOTE_XFORM);
        map.put("down_votes", DOWNVOTE_XFORM);
        map.put("vote_count", VOTECOUNT_XFORM);
        map.put("tally", TALLY_XFORM);
        return map;
    }

    @AllArgsConstructor @Slf4j
    public static class VoteTransformer implements SQLFieldTransformer {
        private String field;
        @Override public Object sqlToObject(Object object, Object input) {
            final SocialEntity social = (SocialEntity) object;
            VoteSummary votes = social.getVotes();
            if (votes == null) {
                votes = new VoteSummary();
                social.setVotes(votes);
            }
            final long count = ((Number) input).longValue();
            switch (field) {
                case "up_votes": votes.setUpVotes(count); break;
                case "down_votes": votes.setDownVotes(count); break;
                case "vote_count": votes.setVoteCount(count); break;
                case "tally": votes.setTally(count); break;
                default: log.warn("sqlToObject: unknown field: "+field);
            }
            return null;
        }
    }
    public static final SQLFieldTransformer UPVOTE_XFORM = new VoteTransformer("up_votes");
    public static final SQLFieldTransformer DOWNVOTE_XFORM = new VoteTransformer("down_votes");
    public static final SQLFieldTransformer VOTECOUNT_XFORM = new VoteTransformer("vote_count");
    public static final SQLFieldTransformer TALLY_XFORM = new VoteTransformer("tally");

}
