package histori.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.shard.Shardable;
import org.cobbzilla.wizard.validation.HasValue;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.Size;

import static histori.ApiConstants.NAME_MAXLEN;
import static histori.model.Nexus.NEXUS_JSONB_TYPE;

@NoArgsConstructor @Accessors(chain=true) @Entity
@Table(uniqueConstraints={
        @UniqueConstraint(columnNames={"owner", "name"}),
        @UniqueConstraint(columnNames={"owner", "source"})
})
public class Feed extends AccountOwnedEntity implements Shardable {

    @Override public String getHashToShardField() { return "owner"; }

    @HasValue(message="err.name.empty")
    @Size(min=3, max=NAME_MAXLEN, message="err.name.length")
    @Column(length=NAME_MAXLEN, nullable=false)
    @Getter @Setter private String name;

    @HasValue(message="err.source.empty")
    @Size(min=3, max=NAME_MAXLEN, message="err.source.length")
    @Column(length=NAME_MAXLEN, nullable=false)
    @Getter @Setter private String source;

    @Size(min=3, max=NAME_MAXLEN, message="err.book.length")
    @Column(length=NAME_MAXLEN)
    @Getter @Setter private String book;

    @Enumerated(EnumType.STRING)
    @Column(length=30, nullable=false)
    @Getter @Setter private FeedPollInterval poll = FeedPollInterval.daily;

    @HasValue(message="err.path.empty")
    @Size(min=3, max=NAME_MAXLEN, message="err.path.length")
    @Column(length=NAME_MAXLEN, nullable=false)
    @Getter @Setter private String path;

    @Size(max=NAME_MAXLEN, message="err.match.length")
    @Column(length=NAME_MAXLEN)
    @Getter @Setter private String match;

    @Type(type=NEXUS_JSONB_TYPE)
    @Getter @Setter private Nexus nexus = new Nexus();

}
