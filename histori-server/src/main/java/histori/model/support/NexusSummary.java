package histori.model.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import histori.model.Nexus;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.dao.SearchResults;

public class NexusSummary {

    public static final JavaType SEARCH_RESULT_TYPE = new NexusSummary().getSearchResultType();
    @JsonIgnore public JavaType getSearchResultType() { return SearchResults.jsonType(getClass()); }

    @Getter @Setter private Nexus primary;

    @Getter @Setter private NexusTagSummary[] tags;

    // UUIDs of other Nexuses with the same name, first 100
    @Getter @Setter private String[] others;

    // Number of nexuses with this name, in total
    @Getter @Setter private int totalCount;

}
