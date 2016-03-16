package histori.model;

import histori.model.base.NexusBase;
import org.cobbzilla.wizard.model.shard.Shardable;

import javax.persistence.*;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(name = "nexus_uniq", columnNames = {"owner", "name"}))
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class Nexus extends NexusBase implements Shardable {

    @Override public String getHashToShardField() { return "canonicalName"; }

}
