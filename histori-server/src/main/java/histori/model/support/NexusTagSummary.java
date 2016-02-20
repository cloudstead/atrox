package histori.model.support;

import histori.model.NexusTag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
public class NexusTagSummary {

    @Getter @Setter private NexusTag tag;
    @Getter @Setter private int count;

}
