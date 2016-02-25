package histori.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
@Embeddable
public class GeoBounds {

    @Column(nullable=false) @HasValue(message="err.north.empty")
    @Getter @Setter private double north;

    @Column(nullable=false) @HasValue(message="err.south.empty")
    @Getter @Setter private double south;

    @Column(nullable=false) @HasValue(message="err.east.empty")
    @Getter @Setter private double east;

    @Column(nullable=false) @HasValue(message="err.west.empty")
    @Getter @Setter private double west;

    public static GeoBounds blank() {
        return new GeoBounds(Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE);
    }

    public void expandToFit (double lat, double lon) {
        if (lat > north) north = lat;
        if (lat < south) south = lat;
        if (lon > east) east = lon;
        if (lon < west) west = lon;
    }
}
