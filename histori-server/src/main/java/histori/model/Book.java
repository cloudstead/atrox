package histori.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.shard.Shardable;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.Size;

import static histori.ApiConstants.NAME_MAXLEN;

@NoArgsConstructor @Accessors(chain=true) @Entity
public class Book extends AccountOwnedEntity implements Shardable {

    @Override public String getHashToShardField() { return "name"; }

    @HasValue(message="err.name.empty")
    @Size(min=3, max=NAME_MAXLEN, message="err.name.length")
    @Column(length=NAME_MAXLEN, nullable=false, unique=true)
    @Getter @Setter private String name;

    @HasValue(message="err.shortName.empty")
    @Size(min=3, max=100, message="err.shortName.length")
    @Column(length=100, nullable=false, unique=true)
    @Getter @Setter private String shortName;

}
