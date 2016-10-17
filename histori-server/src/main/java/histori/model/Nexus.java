package histori.model;

import histori.model.base.NexusBase;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.model.json.JSONBUserType;
import org.cobbzilla.wizard.model.shard.Shardable;

import javax.persistence.*;

@Entity @Slf4j
@Table(uniqueConstraints = @UniqueConstraint(name = "nexus_uniq", columnNames = {"owner", "name"}))
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Nexus extends NexusBase implements Shardable {

    public static final String NEXUS_JSONB_TYPE = JSONBUserType.JSONB_TYPE+"_Nexus";

    @Override public String getHashToShardField() { return "canonicalName"; }

}
