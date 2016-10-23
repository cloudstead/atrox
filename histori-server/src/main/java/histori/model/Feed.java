package histori.model;

import histori.model.template.NexusTemplate;
import histori.server.HistoriConfiguration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.shard.Shardable;
import org.cobbzilla.wizard.validation.HasValue;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.util.List;

import static histori.ApiConstants.NAME_MAXLEN;
import static histori.model.template.NexusTemplate.NEXUS_TEMPLATE_JSONB_TYPE;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@NoArgsConstructor @Accessors(chain=true) @Entity
@Table(uniqueConstraints={
        @UniqueConstraint(columnNames={"owner", "name"  }, name="feed_uniq_owner_name")
})
public class Feed extends AccountOwnedEntity implements Shardable {

    @Override public String getHashToShardField() { return "owner"; }

    private static final String[] VALUE_FIELDS = {"name", "book", "poll", "source", "reader", "path", "match", "nexus"};

    public Feed(Feed other) { update(other); }

    @Override public void update(Identifiable other) { copy(this, other, VALUE_FIELDS); }

    @HasValue(message="err.name.empty")
    @Size(min=3, max=NAME_MAXLEN, message="err.name.length")
    @Column(length=NAME_MAXLEN, nullable=false)
    @Getter @Setter private String name;

    @Size(min=3, max=NAME_MAXLEN, message="err.book.length")
    @Column(length=NAME_MAXLEN)
    @Getter @Setter private String book;

    @Enumerated(EnumType.STRING)
    @Column(length=30, nullable=false)
    @Getter @Setter private FeedPollInterval poll = FeedPollInterval.daily;

    @HasValue(message="err.source.empty")
    @Size(min=3, max=NAME_MAXLEN, message="err.source.length")
    @Column(length=NAME_MAXLEN, nullable=false)
    @Getter @Setter private String source;

    @HasValue(message="err.reader.empty")
    @Size(min=3, max=NAME_MAXLEN, message="err.reader.length")
    @Column(length=NAME_MAXLEN, nullable=false)
    @Getter @Setter private String reader;

    public FeedReader getFeedReader(HistoriConfiguration configuration) {
        final FeedReader reader = instantiate(getReader());
        return configuration.autowire(reader);
    }
    public List<Nexus> read(HistoriConfiguration configuration) { return getFeedReader(configuration).read(this); }

    @HasValue(message="err.path.empty")
    @Size(min=3, max=NAME_MAXLEN, message="err.path.length")
    @Column(length=NAME_MAXLEN, nullable=false)
    @Getter @Setter private String path;

    @Size(max=NAME_MAXLEN, message="err.match.length")
    @Column(length=NAME_MAXLEN)
    @Getter @Setter private String match;
    public boolean hasMatch () { return !empty(match); }

    @Type(type=NEXUS_TEMPLATE_JSONB_TYPE)
    @Getter @Setter private NexusTemplate nexus = new NexusTemplate();

    @Transient @Getter @Setter List<Nexus> nexuses;

}
