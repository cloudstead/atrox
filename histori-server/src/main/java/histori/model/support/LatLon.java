package histori.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.math.Cardinal;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class LatLon {

    @Getter @Setter private double lat;
    @Getter @Setter private double lon;

    public LatLon(String coordinate) {
        int commaPos = coordinate.indexOf(',');
        if (commaPos == -1 || commaPos == coordinate.length()-1) die("LatLon: invalid value: "+coordinate);
        setLat(Double.parseDouble(coordinate.substring(0, commaPos).trim()));
        setLon(Double.parseDouble(coordinate.substring(commaPos+1).trim()));
    }

    public LatLon(int latDeg, Integer latMin, Integer latSec, Cardinal latCardinal,
                  int lonDeg, Integer lonMin, Integer lonSec, Cardinal lonCardinal) {
        this((double) latDeg, latMin == null ? null : latMin.doubleValue(), latSec == null ? null : latSec.doubleValue(), latCardinal,
             (double) lonDeg, lonMin == null ? null : lonMin.doubleValue(), lonSec == null ? null : lonSec.doubleValue(), lonCardinal);
    }

    public LatLon(double latDeg, Double latMin, Double latSec, Cardinal latCardinal,
                  double lonDeg, Double lonMin, Double lonSec, Cardinal lonCardinal) {
        this.lat = latDeg;
        if (latMin != null) this.lat += latMin / 60.0d;
        if (latSec != null) this.lat += latSec / 3600.0d;
        this.lat *= latCardinal.getDirection();

        this.lon = lonDeg;
        if (lonMin != null) this.lon += lonMin / 60.0d;
        if (lonSec != null) this.lon += lonSec / 3600.0d;
        this.lon *= lonCardinal.getDirection();
    }
}
