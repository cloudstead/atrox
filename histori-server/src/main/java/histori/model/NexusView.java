package histori.model;

import java.util.List;

public interface NexusView {

    String getUuid();

    String getName();

    boolean hasNexusType();

    String getNexusType();

    boolean hasTags();

    List<NexusTag> getTags();
}
