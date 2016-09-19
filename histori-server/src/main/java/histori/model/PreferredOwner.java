package histori.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.shard.Shardable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Comparator;

@Entity @NoArgsConstructor @Accessors(chain=true)
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"owner", "preferred"}, name="preferred_owner_UNIQ_owner_preferred"))
public class PreferredOwner extends SpecialAuthorEntity implements Shardable {

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

    @Override public void update(Identifiable thing) {
        final PreferredOwner other = (PreferredOwner) thing;
        setPriority(other.getPriority());
        super.update(thing);
    }

    @Column(length=UUID_MAXLEN, nullable=false)
    @Getter @Setter private String preferred;

    @Getter @Setter private int priority = 0;

    @Override public String getSpecialAuthor() { return getPreferred(); }
    @Override public SpecialAuthorEntity setSpecialAuthor(String author) { return setPreferred(author); }
}
