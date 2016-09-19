package histori.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.shard.Shardable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity @NoArgsConstructor @Accessors(chain=true)
@Table(uniqueConstraints=@UniqueConstraint(columnNames={"owner", "blocked"}, name="blocked_owner_UNIQ_owner_blocked"))
public class BlockedOwner extends SpecialAuthorEntity implements Shardable {

    @Column(length=UUID_MAXLEN, nullable=false)
    @Getter @Setter private String blocked;

    @Override public String getSpecialAuthor() { return getBlocked(); }
    @Override public SpecialAuthorEntity setSpecialAuthor(String author) { return setBlocked(author); }

}
