package histori.model.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Enumerated;

import static javax.persistence.EnumType.STRING;

@Embeddable @NoArgsConstructor @Accessors(chain=true)
public class SearchQuery {

    @Column(length=1024, updatable=false)
    @Getter @Setter private String query;

    @HasValue(message="err.search.from.empty")
    @Column(length=30, nullable=false, updatable=false)
    @Getter @Setter private String from;

    @HasValue(message="err.search.to.empty")
    @Column(length=30, nullable=false, updatable=false)
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

