package atrox.model.support;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

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

}
