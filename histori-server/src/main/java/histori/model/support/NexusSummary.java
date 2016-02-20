package histori.model.support;

import histori.model.Nexus;
import lombok.Getter;
import lombok.Setter;

public class NexusSummary {

    @Getter @Setter private Nexus primary;

    @Getter @Setter private NexusTagSummary[] tags;

    // UUIDs of other Nexuses with the same name, first 100
    @Getter @Setter private String[] others;

    // Number of nexuses with this name, in total
    @Getter @Setter private int totalCount;

}
