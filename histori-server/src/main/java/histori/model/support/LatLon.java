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

    public LatLon(double latDeg, double latMin, double latSec, Cardinal latCardinal,
                  double lonDeg, double lonMin, double lonSec, Cardinal lonCardinal) {
        this.lat = latCardinal.getDirection() * ((latSec + 60*latMin)/3600 + latDeg);
        this.lon = lonCardinal.getDirection() * ((lonSec + 60*lonMin)/3600 + lonDeg);
    }
}
