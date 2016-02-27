package histori.wiki.finder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class InfoboxNames {

    public static final String BOXNAME_COORD = "Coord";

    public static final String INFOBOX_MILITARY_CONFLICT = "Infobox military conflict";
    public static final String INFOBOX_PROTECTED_AREA = "Infobox protected area";
    public static final String INFOBOX_LOCATION_SUFFIX = " location";
    public static final String INFOBOX_SITE_SUFFIX = " site";
    public static final String INFOBOX_CITY_SUFFIX = " city";
    public static final String INFOBOX_MUNICIPALITY_SUFFIX = " municipality";
    public static final String INFOBOX_SETTLEMENT_SUFFIX = " settlement";
    public static final String INFOBOX_COMMUNE_SUFFIX = " commune";

    public static final Set<String> COORD_BOX_CANDIDATES = new HashSet<>(Arrays.asList(
            FinderBase.normalizeInfoboxName(INFOBOX_MILITARY_CONFLICT),
            FinderBase.normalizeInfoboxName(BOXNAME_COORD)
    ));

    public static boolean isCoordinateInfoboxCandidate(String name) {
        return COORD_BOX_CANDIDATES.contains(FinderBase.normalizeInfoboxName(name))
                || isLocationInfobox(name);
    }

    public static final Set<String> LOCATION_CANDIDATES = new HashSet<>(Arrays.asList(
            FinderBase.normalizeInfoboxName(INFOBOX_PROTECTED_AREA)
    ));

    public static boolean isLocationInfobox(String name) {
        final String nameLower = name.trim().toLowerCase();
        return LOCATION_CANDIDATES.contains(FinderBase.normalizeInfoboxName(name))
                || nameLower.endsWith(INFOBOX_LOCATION_SUFFIX)
                || nameLower.endsWith(INFOBOX_MUNICIPALITY_SUFFIX)
                || nameLower.endsWith(INFOBOX_SITE_SUFFIX)
                || nameLower.endsWith(INFOBOX_CITY_SUFFIX)
                || nameLower.endsWith(INFOBOX_SETTLEMENT_SUFFIX)
                || nameLower.endsWith(INFOBOX_COMMUNE_SUFFIX);
    }

    public static boolean lineMatchesLocationInfobox(String line) {
        final String lineLower = line.trim().toLowerCase();
        int infoboxIndex = lineLower.indexOf("{{infobox");
        if (infoboxIndex == -1) return false;
        final StringBuilder infoboxName = new StringBuilder();
        for (int i=infoboxIndex; i<line.length(); i++) {
            char c = line.charAt(i);
            if (c != '\n' && c != '|') {
                infoboxName.append(c);
            } else {
                break;
            }
        }
        return isLocationInfobox(infoboxName.toString().trim());
    }
}
