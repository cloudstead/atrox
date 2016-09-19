package histori.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.shard.Shardable;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@MappedSuperclass @NoArgsConstructor @Accessors(chain=true)
public abstract class SpecialAuthorEntity extends AccountOwnedEntity implements Shardable {

    @Override public String getHashToShardField() { return "owner"; }

    @Override public void update(Identifiable thing) {
        final SpecialAuthorEntity other = (SpecialAuthorEntity) thing;
        setActive(other.isActive());
    }

    @Getter @Setter private boolean active;

    @Transient @Setter private String name;
    public String getName() { return empty(name) ? null : name.trim(); }

    @Transient @JsonIgnore public abstract String getSpecialAuthor();
    public abstract SpecialAuthorEntity setSpecialAuthor(String author);

    public SpecialAuthorEntity toggleActive() { setActive(!isActive()); return this; }

}
