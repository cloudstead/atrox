package histori.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import histori.model.Account;
import histori.model.Nexus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ExpirableBase;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

import static histori.model.support.EntityVisibility.everyone;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;

@Accessors(chain=true) @NoArgsConstructor
public class NexusSummary extends ExpirableBase {

    public static final JavaType SEARCH_RESULT_TYPE = new NexusSummary().getSearchResultType();

    public NexusSummary (String uuid) { setUuid(uuid); }

    public static NexusSummary simpleSummary(SortedSet<Nexus> group) {
        boolean first = true;
        final List<String> others = new ArrayList<>();
        for (Nexus n : group) {
            if (!first) others.add(n.getUuid());
            first = false;
        }

        return new NexusSummary("incomplete-"+summaryUuid(group.first()))
                .setIncomplete(true)
                .setPrimary(group.first())
                .setOthers(others.toArray(new String[others.size()]))
                .setTotalCount(group.size());
    }

    public static NexusSummary simpleSummary(Nexus nexus) {
        return new NexusSummary("incomplete-"+summaryUuid(nexus))
                .setIncomplete(true)
                .setPrimary(nexus);
    }

    public static String summaryUuid(SortedSet<Nexus> group, Account account, EntityVisibility visibility) {
        return summaryUuid(group.first(), account, visibility);
    }

    public static String summaryUuid(Nexus nexus, Account account, EntityVisibility visibility) {
        return "account:" + (account == null ? "null" : account.getUuid())
                + "-" + sha256_hex(nexus.getName())
                + "-" + visibility.name();
    }

    public static String summaryUuid(Nexus nexus) { return summaryUuid(nexus, null, everyone); }

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
