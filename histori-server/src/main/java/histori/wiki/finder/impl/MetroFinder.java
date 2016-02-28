package histori.wiki.finder.impl;

import histori.model.TagType;
import histori.model.support.LatLon;
import histori.model.support.NexusRequest;
import histori.model.support.TimeRange;
import histori.wiki.WikiDateFormat;
import histori.wiki.WikiNode;
import histori.wiki.finder.DateRangeFinder;
import histori.wiki.finder.MultiNexusFinder;
import histori.wiki.finder.TextEventFinder;
import histori.wiki.matcher.LocationInfoboxMatcher;
import lombok.extern.slf4j.Slf4j;
import org.geojson.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static histori.wiki.finder.TextEventFinder.DATE_GROUP;

@Slf4j
public class MetroFinder extends MultiNexusFinder {

    public static final LocationInfoboxMatcher LOCATION_MATCHER = new LocationInfoboxMatcher();
    public static final String ESTABLISHED_DATE = "established_date";
    public static final String ESTABLISHED_TITLE = "established_title";
    public static final String HISTORY_HEADER = "==History==\n";

    @Override
    public List<NexusRequest> find() {
        final WikiNode infobox = article.findFirstInfoboxMatch(LOCATION_MATCHER);
        if (infobox == null || !infobox.hasChildren()) return null;

        LatLon coordinates = getLatLon(wiki);
        if (coordinates == null) return null;

        final Map<String, NexusRequest> requests = new HashMap<>();
        for (WikiNode child : infobox.getChildren()) {
            if (child.getType().isAttribute() && child.getName().toLowerCase().startsWith("established_")) {
                if (child.getName().toLowerCase().startsWith(ESTABLISHED_DATE)) {

                    final NexusRequest r = getNexusRequest(requests, child, ESTABLISHED_DATE);
                    TimeRange range = null;
                    final WikiNode startDateBox = child.findFirstInfoboxWithName("start date");
                    if (startDateBox != null) {
                        range = DateRangeFinder.fromStartDateInfobox(startDateBox);
                    }
                    if (range == null) {
                        range = WikiDateFormat.parse(child.findAllChildText());
                    }
                    if (range == null) {
                        log.warn("Unparseable range: " + child.findAllChildText());
                    } else {
                        r.setTimeRange(range);
                    }

                } else if (child.getName().toLowerCase().startsWith(ESTABLISHED_TITLE)) {

                    final NexusRequest r = getNexusRequest(requests, child, ESTABLISHED_TITLE);
                    r.setName(article.getName() + " " + child.findAllChildTextButNotLinkDescriptions());

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
                    log.warn("Unrecognized 'established' attribute: " + child.getName());
                }
            }
        }

        final ArrayList<NexusRequest> found = new ArrayList<>();
        for (NexusRequest r : requests.values()) {
            if (r.hasName() && r.hasRange()) found.add(r);
        }

        if (found.isEmpty()) {
            // Look in (1) opening paragraph of the article and (2) first paragraph of "History" section, if there is one
            final String firstParagraph = getIntroduction();
            final String historySection = getHistoryParagraph();

            NexusRequest request;
            request = findFoundingDate(historySection);
            if (request == null) request = findFoundingDate(firstParagraph);
            if (request != null) found.add(request);
        }

        for (NexusRequest r : found) {
            r.setPoint(new Point(coordinates.getLon(), coordinates.getLat()));
            r.setNexusType("founding");
            r.addTag("founding", TagType.EVENT_TYPE);
        }
        return found;
    }

    public static final TextEventFinder[] TEXT_FINDERS = new TextEventFinder[] {
            new TextEventFinder("first\\s+.*?settlers?.+?arrived\\s+(?:in|on)\\s+"+ DATE_GROUP, "settled"),
            new TextEventFinder("was formed\\s+(?:in|on)\\s+"+ DATE_GROUP, "formed"),
            new TextEventFinder("founded\\s+by\\s+.+?in\\s+the\\s+(.+?\\s+century)", "founded"),
            new TextEventFinder("in\\s+"+DATE_GROUP+"\\s.+?(:?formed by|founded by)", "founded")
    };

    private NexusRequest findFoundingDate(String paragraph) {
        TimeRange range = null;
        TextEventFinder matched = null;
        for (TextEventFinder finder : TEXT_FINDERS) {
            range = finder.find(paragraph);
            if (range != null) {
                matched = finder;
                break;
            }
        }
        return range == null ? null : (NexusRequest) new NexusRequest().setTimeRange(range).setName(article.getName()+" "+matched.getDescription());
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

    private String getIntroduction() {
        final StringBuilder b = new StringBuilder();
        boolean found = false;
        for (WikiNode node : article.getChildren()) {
            final String nodeName = node.getName();
            if (node.getType().isString() && nodeName.startsWith("'''")) {
                found = true;
            }
            if (found) {
                if (appendParagraphNode(b, node, nodeName, "==")) break;
            }
        }
        return b.toString();
    }


    private String getHistoryParagraph() {
        final StringBuilder b = new StringBuilder();
        boolean found = false;
        String terminator = null;
        for (WikiNode node : article.getChildren()) {
            String nodeName = node.getName();
            if (node.getType().isString() && nodeName.contains(HISTORY_HEADER)) {
                found = true;
                nodeName = nodeName.substring(nodeName.indexOf(HISTORY_HEADER));
            }
            if (found) {
                if (appendParagraphNode(b, node, nodeName, terminator)) break;
                terminator = "==";
            }
        }
        return b.toString();
    }

    private boolean appendParagraphNode(StringBuilder b, WikiNode node, String nodeName, String terminator) {
        if (node.getType().isString()) {
            if (terminator != null && nodeName.contains(terminator)) {
                b.append(nodeName.substring(0, nodeName.indexOf(terminator)));
                return true;
            }
            b.append(nodeName).append(" ");

        } else if (node.getType().isLink()) {
            if (node.hasChildren()) {
                b.append(node.firstChildName()).append(" ");
            } else {
                b.append(nodeName).append(" ");
            }
        }
        return false;
    }

}
