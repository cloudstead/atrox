package histori.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import histori.model.Nexus;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ExpirableBase;

import java.util.SortedSet;

@Accessors(chain=true)
public class NexusSummary extends ExpirableBase {

    public static final JavaType SEARCH_RESULT_TYPE = new NexusSummary().getSearchResultType();

    public static NexusSummary simpleSummary(SortedSet<Nexus> group) {
        return new NexusSummary().setIncomplete(true).setPrimary(group.first()).setTotalCount(group.size());
    }

    public static NexusSummary simpleSummary(Nexus nexus) {
        return new NexusSummary().setIncomplete(true).setPrimary(nexus);
    }

    @JsonIgnore public JavaType getSearchResultType() { return SearchResults.jsonType(getClass()); }

    @Getter @Setter private Nexus primary;

    @Getter @Setter private NexusTagSummary[] tags;

    // UUIDs of other Nexuses with the same name, first 100
    @Getter @Setter private String[] others;

    // Number of nexuses with this name, in total
    @Getter @Setter private int totalCount;

    // If true, this is a stub and the client can ask for the summary again to receive more data
    @Getter @Setter private boolean incomplete;

}
