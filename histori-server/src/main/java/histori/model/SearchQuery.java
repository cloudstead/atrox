package histori.model;

import histori.model.support.EntityVisibility;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.model.IdentifiableBase;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Enumerated;

import static javax.persistence.EnumType.STRING;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

@Entity @NoArgsConstructor @Accessors(chain=true)
@EqualsAndHashCode(of={"query", "visibility", "from", "to", "north", "south", "east", "west"}, callSuper=false)
public class SearchQuery extends IdentifiableBase {

    public SearchQuery (SearchQuery other) { copy(this, other); setUuid(null); }

    @Column(length=1024, updatable=false)
    @Getter @Setter private String query;

    @Getter @Setter private boolean useCache = true;

    @HasValue(message="err.search.from.empty")
    @Column(length=30, nullable=false, updatable=false, name="tStart")
    @Getter @Setter private String from;

    @HasValue(message="err.search.to.empty")
    @Column(length=30, nullable=false, updatable=false, name="tEnd")
    @Getter @Setter private String to;

    @HasValue(message="err.search.north.empty")
    @Column(nullable=false, updatable=false)
    @Getter @Setter private double north;

    @HasValue(message="err.search.south.empty")
    @Column(nullable=false, updatable=false)
    @Getter @Setter private double south;

    @HasValue(message="err.search.east.empty")
    @Column(nullable=false, updatable=false)
    @Getter @Setter private double east;

    @HasValue(message="err.search.west.empty")
    @Column(nullable=false, updatable=false)
    @Getter @Setter private double west;

    @Enumerated(value=STRING)
    @Getter @Setter private EntityVisibility visibility;

}

