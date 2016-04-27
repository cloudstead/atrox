package histori.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import histori.model.Nexus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ExpirableBase;

import java.util.Comparator;

@Accessors(chain=true) @NoArgsConstructor
public class NexusSummary extends ExpirableBase {

    public static final JavaType SEARCH_RESULT_TYPE = new NexusSummary().getSearchResultType();

    public static Comparator<NexusSummary> comparator (SearchSortOrder sort) {
        switch (sort) {
            case newest:              return NSCompare_newest.instance;
            case oldest:              return NSCompare_oldest.instance;
            case up_vote:             return NSCompare_up_vote.instance;
            case down_vote:           return NSCompare_down_vote.instance;
            case vote_count:          return NSCompare_vote_count.instance;
            case vote_tally: default: return NSCompare_vote_tally.instance;
        }
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NexusSummary summary = (NexusSummary) o;
        return primary.getCanonicalName().equals(summary.primary.getCanonicalName());
    }

    @Override public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + primary.getCanonicalName().hashCode();
        return result;
    }

    @JsonIgnore public JavaType getSearchResultType() { return SearchResults.jsonType(getClass()); }

    @Getter @Setter private Nexus primary;
    @Getter @Setter private boolean incomplete;

    @Getter @Setter private NexusTagSummary[] tags;

    // UUIDs of other Nexuses with the same name, first 100
    @Getter @Setter private String[] others;

    // Number of nexuses with this name, in total
    @Getter @Setter private int totalCount;

    @Override public String toString() {
        return "NexusSummary{primary=" + primary.getCanonicalName() + ", others="+(others == null ? 0 : others.length)+"}";
    }

    private static abstract class NSCompare implements Comparator<NexusSummary> {
        @Override public int compare(NexusSummary o1, NexusSummary o2) {
            long v1 = val(o1);
            long v2 = val(o2);
            return reverse() ? Long.compare(v1, v2) : Long.compare(v2, v1);
        }
        protected abstract long val(NexusSummary ns);
        protected boolean reverse () { return false; }
    }

    private static class NSCompare_newest extends NSCompare {
        static final NSCompare_newest instance = new NSCompare_newest();
        @Override protected long val(NexusSummary ns) { return ns.getCtime(); }
    }
    private static class NSCompare_oldest extends NSCompare {
        static final NSCompare_oldest instance = new NSCompare_oldest();
        @Override protected long val(NexusSummary ns) { return ns.getCtime(); }
        @Override protected boolean reverse() { return true; }
    }
    private static class NSCompare_up_vote extends NSCompare {
        static final NSCompare_up_vote instance = new NSCompare_up_vote();
        @Override protected long val(NexusSummary ns) { return ns.getPrimary().hasVotes() ? ns.getPrimary().getVotes().getUpVotes() : 0; }
    }
    private static class NSCompare_down_vote extends NSCompare {
        static final NSCompare_down_vote instance = new NSCompare_down_vote();
        @Override protected long val(NexusSummary ns) { return ns.getPrimary().hasVotes() ? ns.getPrimary().getVotes().getDownVotes() : 0; }
    }
    private static class NSCompare_vote_count extends NSCompare {
        static final NSCompare_vote_count instance = new NSCompare_vote_count();
        @Override protected long val(NexusSummary ns) { return ns.getPrimary().hasVotes() ? ns.getPrimary().getVotes().getVoteCount() : 0; }
    }
    private static class NSCompare_vote_tally extends NSCompare {
        static final NSCompare_vote_tally instance = new NSCompare_vote_tally();
        @Override protected long val(NexusSummary ns) { return ns.getPrimary().hasVotes() ? ns.getPrimary().getVotes().getTally() : 0; }
    }
}
