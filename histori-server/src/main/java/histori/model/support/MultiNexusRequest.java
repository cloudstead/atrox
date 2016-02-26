package histori.model.support;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Accessors(chain=true)
public class MultiNexusRequest extends NexusRequest {

    @Getter @Setter private List<NexusRequest> requests = new ArrayList<>();

    public boolean hasRequests() { return !requests.isEmpty(); }

    public MultiNexusRequest add (NexusRequest r) { requests.add(r); return this; }

}
