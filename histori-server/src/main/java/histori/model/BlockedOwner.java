package histori.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.shard.Shardable;

import javax.persistence.*;

@Entity @NoArgsConstructor @Accessors(chain=true)
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"owner", "blocked"}, name="blocked_owner_UNIQ_owner_blocked"))
public class BlockedOwner extends AccountOwnedEntity implements Shardable {

    @Override public String getHashToShardField() { return "owner"; }

    @Column(length=UUID_MAXLEN, nullable=false)
    @Getter @Setter private String blocked;

    @Transient @Getter @Setter private String name;

}
