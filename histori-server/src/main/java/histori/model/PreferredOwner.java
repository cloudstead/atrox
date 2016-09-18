package histori.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.shard.Shardable;

import javax.persistence.*;
import java.util.Comparator;

@Entity @NoArgsConstructor @Accessors(chain=true)
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"owner", "preferred"}, name="preferred_owner_UNIQ_owner_preferred"))
public class PreferredOwner extends AccountOwnedEntity implements Shardable {

    @Override public String getHashToShardField() { return "owner"; }

    public static final Comparator<PreferredOwner> SORT_PRIORITY = new Comparator<PreferredOwner>() {
        @Override public int compare(PreferredOwner o1, PreferredOwner o2) {
            return Integer.compare(o1.getPriority(), o2.getPriority());
        }
    };

    public PreferredOwner (Account owner, Account preferred) {
        setOwner(owner.getUuid());
        setPreferred(preferred.getUuid());
        setName(preferred.getName());
    }

    @Column(length=UUID_MAXLEN, nullable=false)
    @Getter @Setter private String preferred;

    @Transient @Getter @Setter private String name;

    @Getter @Setter private int priority = 0;

}
