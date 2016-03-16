package histori.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.cobbzilla.wizard.model.shard.Shardable;

public interface VersionedEntity extends Shardable {

    @JsonIgnore String[] getIdentifiers ();
    @JsonIgnore String[] getIdentifierFields ();

    int getVersion ();
    VersionedEntity setVersion (int version);

}
