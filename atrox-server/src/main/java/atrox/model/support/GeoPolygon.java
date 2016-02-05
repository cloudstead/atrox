package atrox.model.support;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Embeddable @NoArgsConstructor
public class GeoPolygon {

    public static final String GP_SEP = "|";

    @Column(length=1_000_000, updatable=false, nullable=false)
    @Getter @Setter private String coordinates;

    public GeoPolygon (String coords) { setCoordinates(coords); }

    @Transient
    public List<LatLon> getPoints () {
        final List<LatLon> points = new ArrayList<>();
        if (!empty(coordinates)) {
            for (String coord : coordinates.split(GP_SEP)) {
                points.add(new LatLon(coord));
            }
        }
        return points;
    }

}
