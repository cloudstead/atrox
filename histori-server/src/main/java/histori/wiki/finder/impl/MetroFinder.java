package histori.wiki.finder.impl;

import histori.model.TagType;
import histori.model.support.LatLon;
import histori.model.support.NexusRequest;
import histori.model.support.TimeRange;
import histori.wiki.WikiDateFormat;
import histori.wiki.WikiNode;
import histori.wiki.WikiNodeType;
import histori.wiki.finder.DateRangeFinder;
import histori.wiki.finder.MultiNexusFinder;
import histori.wiki.finder.TextEventFinder;
import histori.wiki.finder.TextEventFinderResult;
import histori.wiki.matcher.LocationInfoboxMatcher;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.string.StringUtil;
import org.geojson.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class MetroFinder extends MultiNexusFinder {

    public static final LocationInfoboxMatcher LOCATION_MATCHER = new LocationInfoboxMatcher();
    public static final String ESTABLISHED_DATE = "established_date";
    public static final String ESTABLISHED_TITLE = "established_title";

    public static final String HISTORY_HEADER = "==History==";
    public static final Pattern INTRO_TERMINATOR = Pattern.compile("==");
    public static final Pattern HISTORY_TERMINATOR = Pattern.compile("==\\w+[^=]+==");

    @Override
    public List<NexusRequest> find() {
        final WikiNode infobox = article.findFirstInfoboxMatch(LOCATION_MATCHER);
        if (infobox == null || !infobox.hasChildren()) return null;

        LatLon coordinates = getLatLon(wiki);
        if (coordinates == null) return null;

        final Map<String, NexusRequest> requests = new HashMap<>();
        for (WikiNode child : infobox.getChildren()) {
            if (child.getType().isAttribute() && child.getName().toLowerCase().startsWith("established_")) {
                final String childText = child.findAllChildText();
                if (child.getName().toLowerCase().startsWith(ESTABLISHED_DATE)) {

                    final NexusRequest r = getNexusRequest(requests, child, ESTABLISHED_DATE);
                    TimeRange range = null;
                    final WikiNode startDateBox = child.findFirstInfoboxWithName("start date");
                    if (startDateBox != null) {
                        range = DateRangeFinder.fromStartDateInfobox(startDateBox);
                    }
                    if (range == null) {
                        range = WikiDateFormat.parse(childText);
                    }
                    if (range == null) {
                        log.warn("Unparseable range: " + childText);
                    } else {
                        r.setTimeRange(range);
                    }

                } else if (child.getName().toLowerCase().startsWith(ESTABLISHED_TITLE)) {

                    if (childText == null) continue;

                    final NexusRequest r = getNexusRequest(requests, child, ESTABLISHED_TITLE);
                    String establishType;
                    if (childText.contains(" by ") && !child.findByType(WikiNodeType.link).isEmpty()) {
                        establishType = child.findAllChildTextButNotLinkDescriptions();
                    } else {
                        establishType = child.findAllChildTextButNotLinkTargets();
                    }
                    if (!establishType.trim().contains(" ")) establishType = StringUtil.uncapitalize(establishType);
                    r.setName(article.getName() + " " + establishType);

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
            final String introduction = getIntroduction();
            final String historySection = getHistoryParagraph();

            TextEventFinderResult finderResult = TextEventFinder.find(historySection);
            if (finderResult == null) finderResult = TextEventFinder.find(introduction);
            if (finderResult != null) {
                NexusRequest request = (NexusRequest) new NexusRequest()
                        .setTimeRange(finderResult.getRange())
                        .setName(article.getName() + " " + finderResult.getDescription());
                found.add(request);
            }
        }

        for (NexusRequest r : found) {
            r.setPoint(new Point(coordinates.getLon(), coordinates.getLat()));
            r.setNexusType("founding");
            r.addTag("founding", TagType.EVENT_TYPE);
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

    private String getIntroduction() {
        final StringBuilder b = new StringBuilder();
        boolean found = false;
        for (WikiNode node : article.getChildren()) {
            final String nodeName = node.getName();
            if (node.getType().isString() && nodeName.startsWith("'''")) {
                found = true;
            }
            if (found) {
                if (appendParagraphNode(b, node, nodeName, INTRO_TERMINATOR)) break;
            }
        }
        return b.toString();
    }


    private String getHistoryParagraph() {
        final StringBuilder b = new StringBuilder();
        boolean found = false;
        Pattern terminator = null;
        for (WikiNode node : article.getChildren()) {
            String nodeName = node.getName();
            if (node.getType().isString() && nodeName.contains(HISTORY_HEADER)) {
                found = true;
                nodeName = nodeName.substring(nodeName.indexOf(HISTORY_HEADER));
            }
            if (found) {
                if (appendParagraphNode(b, node, nodeName, terminator)) break;
                if (b.length() > 100) terminator = HISTORY_TERMINATOR;
            }
        }
        return b.toString();
    }

    private boolean appendParagraphNode(StringBuilder b, WikiNode node, String nodeName, Pattern terminator) {
        if (node.getType().isString()) {
            if (terminator != null) {
                final Matcher matcher = terminator.matcher(nodeName);
                if (matcher.find()) {
                    b.append(nodeName.substring(0, matcher.start(0)));
                    return true;
                }
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
