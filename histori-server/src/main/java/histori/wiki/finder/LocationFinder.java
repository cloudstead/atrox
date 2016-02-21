package histori.wiki.finder;

import histori.model.support.LatLon;
import histori.wiki.ParsedWikiArticle;
import histori.wiki.WikiArchive;
import histori.wiki.WikiNode;
import histori.wiki.WikiNodeType;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.math.Cardinal;

import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@NoArgsConstructor @Accessors(chain=true) @Slf4j
public class LocationFinder extends WikiDataFinderBase<LatLon> {

    public static final String ATTR_COORDINATES = "coordinates";

    public static final String ATTR_LATITUDE = "latitude";
    public static final String ATTR_LONGITUDE = "longitude";

    public static final String ATTR_LATD = "latd";
    public static final String ATTR_LATM = "latm";
    public static final String ATTR_LATS = "lats";
    public static final String ATTR_LATNS = "latNS";
    public static final String ATTR_LONGD = "longd";
    public static final String ATTR_LONGM = "longm";
    public static final String ATTR_LONGS = "longs";
    public static final String ATTR_LONGEW = "longEW";

    public static final String ATTR_PLACE = "place";

    public static final String BATTLE_PREFIX = "battle of ";
    public static final String BOXNAME_COORD = "Coord";

    public LocationFinder(WikiArchive wiki) { super(wiki); }
    public LocationFinder(WikiArchive wiki, ParsedWikiArticle article) { super(wiki, article); }

    public String getLocationNameFromTitle(ParsedWikiArticle art) {
        if (art.getName().toLowerCase().startsWith(BATTLE_PREFIX)) {
            return art.getName().substring(BATTLE_PREFIX.length()).trim();
        }
        return null;
    }

    @Override public LatLon find() { return find(article, false); }

    public LatLon find(ParsedWikiArticle art, boolean coordinatesOnly) {
        // todo: try all of these strategies in parallel? some may be long-running
        LatLon latLon;
        try {
            latLon = parseCoordinates(art);
            if (latLon != null) return latLon;
        } catch (Exception e) {
            log.warn("Attempt at gleaning coordinates from infobox failed: " + e);
        }
        if (coordinatesOnly) return null;

        try {
            latLon = parseTitle(art);
            if (latLon != null) return latLon;
        } catch (Exception e) {
            log.warn("Attempt at gleaning coordinates based on article title failed: " + e);
        }

        try {
            latLon = parsePlace(art);
            if (latLon != null) return latLon;
        } catch (Exception e) {
            log.warn("Attempt at gleaning coordinates based on a place name failed: " + e);
        }
        return null;
    }

    private LatLon parseTitle(ParsedWikiArticle art) {

        final String loc = getLocationNameFromTitle(art);
        if (loc == null) return die("no location could be gleaned from title");

        final ParsedWikiArticle locArticle = getWiki().find(loc);
        if (locArticle == null) return die("location gleaned from title was "+loc+" but locArticle was not found");

        return find(locArticle, true);
    }

    private LatLon parsePlace(ParsedWikiArticle art) {
        for (WikiNode box : art.getInfoboxes()) {
            final WikiNode placeAttr = box.findChildNamed(ATTR_PLACE);
            if (placeAttr != null) {
                final List<WikiNode> links = placeAttr.getLinks();

                // try the first link
                ParsedWikiArticle placeArticle;
                if (!links.isEmpty()) {
                    final String linkName = links.get(0).getName();
                    placeArticle = getWiki().find(linkName);
                    if (placeArticle != null) {
                        LatLon latLon = find(placeArticle, true);
                        if (latLon != null) return latLon;
                    }
                }

                // try the entire place text
                final String placeName = placeAttr.findAllChildText();
                placeArticle = getWiki().find(placeName);
                if (placeArticle != null) {
                    LatLon latLon = find(placeArticle, true);
                    if (latLon != null) return latLon;
                }

                // tokenize by commas and try each combo from shortest to longest
                if (placeName.contains(",")) {
                    final String[] placeParts = placeName.split(",");
                    final StringBuilder nameBuilder = new StringBuilder();
                    for (String part : placeParts) {
                        if (nameBuilder.length() > 0) nameBuilder.append(", ");
                        nameBuilder.append(part.trim());
                        placeArticle = getWiki().find(nameBuilder.toString());
                        if (placeArticle != null) {
                            LatLon latLon = find(placeArticle, true);
                            if (latLon != null) return latLon;
                        }
                    }
                }

            }
        }
        return die("parsePlace: not found");
    }

    private LatLon parseCoordinates(ParsedWikiArticle art) {
        for (WikiNode box : art.getInfoboxes()) {
            if (isIgnoredInfobox(box)) continue;

            if (box.getName().equalsIgnoreCase(BOXNAME_COORD)) {
                try {
                    return fromCoordsValue(box.getChildren());
                } catch (Exception e) {
                    log.warn("Error parsing coords box: "+e, e);
                }
                continue;
            }

            final WikiNode coordsAttr = box.findChildNamed(ATTR_COORDINATES);
            if (coordsAttr != null) {
                try {
                    return parseCoordinates(coordsAttr);
                } catch (Exception ignored) { /* try next thing */ }
            }

            final WikiNode latitude = box.findChildNamed(ATTR_LATITUDE);
            final WikiNode longitude = box.findChildNamed(ATTR_LONGITUDE);
            if (latitude != null && longitude != null) {
                try {
                    return new LatLon(Double.parseDouble(latitude.findAllChildText()), Double.parseDouble(longitude.findAllChildText()));
                } catch (Exception ignored) { /* try next thing */ }
            }

            final WikiNode latd = box.findChildNamed(ATTR_LATD);
            final WikiNode longd = box.findChildNamed(ATTR_LONGD);
            if (latd != null && longd != null) {
                try {
                    return parseLatdLongd(box);
                } catch (Exception ignored) { /* try next thing */ }
            }
        }
        return die("parseCoordinates: not found");
    }

    private LatLon parseCoordinates (WikiNode coordsAttr) throws Exception {
        WikiNode coordsValue = coordsAttr.getChildren().get(0);
        if (coordsValue.getType() == WikiNodeType.infobox && coordsValue.getName().equals(BOXNAME_COORD)) {
            final List<WikiNode> coordNumbers = coordsValue.getChildren();
            return fromCoordsValue(coordNumbers);
        }
        return die("parseCoordinates: unparseable ("+coordsValue+")");
    }

    private LatLon fromCoordsValue(List<WikiNode> coordNumbers) {
        final Cardinal latCardinal;
        final Double latMin, latSec, lonMin, lonSec;
        final Cardinal lonCardinal;
        int lonStartIndex;
        String val2, val3;

        final double latDeg = Double.valueOf(coordNumbers.get(0).findAllText());
        val2 = coordNumbers.get(1).findAllText();
        if (Cardinal.isCardinal(val2)) {
            latCardinal = Cardinal.create(val2);
            latMin = latSec = null;
            lonStartIndex = 2;

        } else {
            latMin = Double.parseDouble(val2);
            val3 = coordNumbers.get(2).findAllText();
            if (Cardinal.isCardinal(val3)) {
                latCardinal = Cardinal.create(val3);
                latSec = null;
                lonStartIndex = 3;
            } else {
                latSec = Double.parseDouble(val3);
                latCardinal = Cardinal.create(coordNumbers.get(3).findAllText());
                lonStartIndex = 4;
            }
        }

        final double lonDeg = Float.valueOf(coordNumbers.get(lonStartIndex).findAllText());
        val2 = coordNumbers.get(lonStartIndex+1).findAllText();
        if (Cardinal.isCardinal(val2)) {
            lonCardinal = Cardinal.create(val2);
            lonMin = lonSec = null;

        } else {
            lonMin = Double.parseDouble(val2);
            val3 = coordNumbers.get(lonStartIndex+2).findAllText();
            if (Cardinal.isCardinal(val3)) {
                lonCardinal = Cardinal.create(val3);
                lonSec = null;
            } else {
                lonSec = Double.parseDouble(val3);
                lonCardinal = Cardinal.create(coordNumbers.get(lonStartIndex+3).findAllText());
            }
        }

        return new LatLon(latDeg, latMin, latSec, latCardinal, lonDeg, lonMin, lonSec, lonCardinal);
    }

    private LatLon parseLatdLongd(WikiNode box) {
        final WikiNode latd = box.findChildNamed(ATTR_LATD);
        final WikiNode latm = box.findChildNamed(ATTR_LATM);
        final WikiNode lats = box.findChildNamed(ATTR_LATS);
        final WikiNode latns = box.findChildNamed(ATTR_LATNS);

        final WikiNode longd = box.findChildNamed(ATTR_LONGD);
        final WikiNode longm = box.findChildNamed(ATTR_LONGM);
        final WikiNode longs = box.findChildNamed(ATTR_LONGS);
        final WikiNode longew = box.findChildNamed(ATTR_LONGEW);

        double latDeg = parseCoordinate(latd, latm, lats, latns);
        double longDeg = parseCoordinate(longd, longm, longs, longew);

        return new LatLon(latDeg, longDeg);
    }

    private double parseCoordinate(WikiNode deg, WikiNode min, WikiNode sec, WikiNode card) {
        double latDeg = Double.parseDouble(deg.findAllChildText());
        if (min != null) latDeg += Double.parseDouble(min.findAllChildText())/60.0;
        if (sec != null) latDeg += Double.parseDouble(sec.findAllChildText())/3600.0;
        if (card != null) latDeg *= (double) Cardinal.create(card.findAllChildText()).getDirection();
        return latDeg;
    }

}
