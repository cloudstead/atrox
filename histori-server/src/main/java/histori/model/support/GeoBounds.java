package histori.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

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

    // used in elasticsearch
    @Transient public LatLon getTopLeft () { return new LatLon(north, west); }
    public void setTopLeft (LatLon latLon) { /* noop */ }
    @Transient public LatLon getTopRight () { return new LatLon(north, east); }
    public void setTopRight (LatLon latLon) { /* noop */ }
    @Transient public LatLon getBottomLeft () { return new LatLon(south, west); }
    public void setBottomLeft (LatLon latLon) { /* noop */ }
    @Transient public LatLon getBottomRight () { return new LatLon(south, east); }
    public void setBottomRight (LatLon latLon) { /* noop */ }

    public void expandToFit (double lat, double lon) {
        if (lat > north) north = lat;
        if (lat < south) south = lat;
        if (lon > east) east = lon;
        if (lon < west) west = lon;
    }

    // only consider first 5 decimal places of degree, yielding a
    // precision of 1/100,000 of a degree: about one meter at the equator
    public static final double MAX_PRECISION = 10_000d;
    public int norm (double val) { return (int)(val * MAX_PRECISION); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GeoBounds geoBounds = (GeoBounds) o;

        if (norm(geoBounds.north) != norm(north)) return false;
        if (norm(geoBounds.south) != norm(south)) return false;
        if (norm(geoBounds.east) != norm(east)) return false;
        if (norm(geoBounds.west) != norm(west)) return false;
        return true;
    }

    @Override public int hashCode() {
        int result;
        long temp;
        temp = norm(north);
        result = (int) (temp ^ (temp >>> 32));
        temp = norm(south);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = norm(east);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = norm(west);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /**
     * @param bounds the bounds to compare to
     * @return true if the bounds argument has a boundary coordinate that is outside the bounds of this
     */
    public boolean isOutside(GeoBounds bounds) {
        return bounds.getNorth() > getNorth()
            || bounds.getSouth() > getSouth()
            || bounds.getEast()  > getEast()
            || bounds.getWest()  > getWest();
    }

}
