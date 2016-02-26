package histori.wiki.finder.impl;

import histori.model.TagType;
import histori.model.support.LatLon;
import histori.model.support.NexusRequest;
import histori.model.support.TimeRange;
import histori.wiki.WikiDateFormat;
import histori.wiki.WikiNode;
import histori.wiki.finder.MultiNexusFinder;
import histori.wiki.matcher.LocationInfoboxMatcher;
import lombok.extern.slf4j.Slf4j;
import org.geojson.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MetroFinder extends MultiNexusFinder {

    public static final LocationInfoboxMatcher LOCATION_MATCHER = new LocationInfoboxMatcher();
    public static final String ESTABLISHED_DATE = "established_date";
    public static final String ESTABLISHED_TITLE = "established_title";

    @Override public List<NexusRequest> find() {
        final WikiNode infobox = article.findFirstInfoboxMatch(LOCATION_MATCHER);
        if (infobox == null || !infobox.hasChildren()) return null;

        LatLon coordinates = getLatLon(wiki);
        if (coordinates == null) return null;

        final Map<String, NexusRequest> requests = new HashMap<>();
        for (WikiNode child : infobox.getChildren()) {
            if (child.getType().isAttribute() && child.getName().toLowerCase().startsWith("established_")) {
                if (child.getName().toLowerCase().startsWith(ESTABLISHED_DATE)) {

                    final NexusRequest r = getNexusRequest(requests, child, ESTABLISHED_DATE);
                    final TimeRange range = WikiDateFormat.parse(child.findAllChildText());
                    if (range == null) {
                        log.warn("Unparseable range: "+child.findAllChildText());
                    } else {
                        r.setTimeRange(new TimeRange(range.getStartPoint(), range.getEndPoint()));
                    }

                } else if (child.getName().toLowerCase().startsWith(ESTABLISHED_TITLE)) {

                    final NexusRequest r = getNexusRequest(requests, child, ESTABLISHED_TITLE);
                    r.setName(article.getName()+ " "+child.findAllChildTextButNotLinkDescriptions());

                    if (r.getName().contains(" by ") || r.getName().contains(" by the ")) {
                        for (int i = 0; i < child.getChildCount(); i++) {
                            WikiNode titlePart = child.getChild(i);
                            if (titlePart.getType().isString()
                                    && (titlePart.getName().toLowerCase().endsWith(" by") || titlePart.getName().toLowerCase().endsWith(" by the"))
                                    && child.getChildCount() > i + 1
                                    && child.getChild(i + 1).getType().isLink()) {
                                r.addTag(child.getChild(i + 1).getName(), "world_actor", "role", "founder");
                            }
                        }
                    }

                } else {
                    log.warn("Unrecognized 'established' attribute: "+child.getName());
                }
            }
        }

        final ArrayList<NexusRequest> found = new ArrayList<>();
        for (NexusRequest r : requests.values()) {
            if (r.hasName() && r.hasRange()) {
                r.setPoint(new Point(coordinates.getLon(), coordinates.getLat()));
                r.setNexusType("founding");
                r.addTag("founding", TagType.EVENT_TYPE);
                found.add(r);
            }
        }
        return found;
    }

    public NexusRequest getNexusRequest(Map<String, NexusRequest> requests, WikiNode child, String prefix) {
        final String suffix = child.getName().substring(prefix.length());
        NexusRequest r = requests.get(suffix);
        if (r == null) {
            r = new NexusRequest();
            requests.put(suffix, r);
        }
        return r;
    }
}
