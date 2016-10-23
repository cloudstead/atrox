package histori.feed;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import histori.dao.NexusDAO;
import histori.model.Feed;
import histori.model.FeedReader;
import histori.model.Nexus;
import histori.model.NexusTag;
import histori.model.base.NexusTags;
import histori.model.support.TimeRange;
import histori.model.tag_schema.TagSchemaValue;
import histori.model.template.NexusTagTemplate;
import histori.model.template.NexusTemplate;
import histori.server.HistoriConfiguration;
import histori.wiki.WikiDateFormat;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.handlebars.HandlebarsUtil;
import org.cobbzilla.util.http.HttpResponseBean;
import org.cobbzilla.util.http.HttpUtil;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.util.xml.XPathUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.StreamUtil.stream2string;
import static org.cobbzilla.util.json.JsonUtil.json;

@Slf4j
@SuppressWarnings({"SpringJavaAutowiredMembersInspection", "unused"})
public class RssReader implements FeedReader {

    @Autowired private HistoriConfiguration configuration;
    @Autowired private NexusDAO nexusDAO;

    public static final Map<String, Object> EMPTY_CTX = Collections.emptyMap();

    @Override public List<Nexus> read(Feed feed) {

        final String source = feed.getSource();
        final String xml;
        if (source.startsWith("classpath:")) {
            final String path = source.substring("classpath:".length());
            if (!path.startsWith("feeds/")) die("read: invalid source: "+source);
            xml = stream2string(path);
        } else {
            try {
                final HttpResponseBean response = HttpUtil.getResponse(source);
                xml = response.getEntityString();
            } catch (Exception e) {
                return die("processFeed: error fetching source (" + source + "): " + e, e);
            }
        }

        final XPathUtil xpath = new XPathUtil(feed.getPath(), false);
        final List<Node> items;
        try {
            items = xpath.getFirstMatchList(xml);
        } catch (Exception e) {
            return die("read: "+e, e);
        }

        // pre-scan to determine which XPaths we want to get
        final Set<String> xpaths = findXpaths(feed);

        final List<Nexus> nexuses = new ArrayList<>();
        try {
            final XPathUtil itemXpath = new XPathUtil(xpaths, false);
            final Document document = itemXpath.getDocument(xml);
            Map<String, List<Node>> matches;
            for (Node item : items) {
                // Get the XPaths we will need
                matches = itemXpath.applyXPaths(document, item);
                final Handlebars handlebars = getXpathHandlebars(matches);

                if (feed.hasMatch()) {
                    final String matchOk = HandlebarsUtil.apply(handlebars, feed.getMatch(), EMPTY_CTX);
                    if (Boolean.valueOf(matchOk)) {
                        final Nexus nexus = processItem(feed, handlebars);
                        if (nexus != null) nexuses.add(nexus);
                    }
                } else {
                    final Nexus nexus = processItem(feed, handlebars);
                    if (nexus != null) nexuses.add(nexus);
                }
            }

        } catch (Exception e) {
            return die("processFeed: error applying xpaths: " + e, e);
        }

        return nexuses;
    }

    private Nexus processItem(Feed feed, Handlebars handlebars) {

        final NexusTemplate nexusTemplate = feed.getNexus();
        final Nexus nexus = new Nexus();

        nexus.setBook(feed.getBook());
        nexus.setName(HandlebarsUtil.apply(handlebars, nexusTemplate.getName(), EMPTY_CTX));

        final TimeRange timeRange = new TimeRange();
        final String startString = HandlebarsUtil.apply(handlebars, nexusTemplate.getTimeRange().getStart(), EMPTY_CTX);
        timeRange.setStartPoint(WikiDateFormat.parse(startString).getStartPoint());
        final String endString = HandlebarsUtil.apply(handlebars, nexusTemplate.getTimeRange().getStart(), EMPTY_CTX);
        timeRange.setEndPoint(WikiDateFormat.parse(endString).getStartPoint());
        nexus.setTimeRange(timeRange);

        final String geoJson = HandlebarsUtil.apply(handlebars, nexusTemplate.getGeoJson(), EMPTY_CTX);
        nexus.setGeoJson(geoJson);

        if (nexusTemplate.hasMarkdown()) {
            nexus.setMarkdown(HandlebarsUtil.apply(handlebars, nexusTemplate.getMarkdown(), EMPTY_CTX));
        }
        if (nexusTemplate.hasTags()) {
            final List<NexusTag> tags = new ArrayList<>();
            for (NexusTagTemplate tagTemplate : nexusTemplate.getTags()) {

                final String tagName = HandlebarsUtil.apply(handlebars, tagTemplate.getTagName(), EMPTY_CTX);
                final String tagType = HandlebarsUtil.apply(handlebars, tagTemplate.getTagType(), EMPTY_CTX);
                if (empty(tagType) || empty(tagName)) continue;

                if (tagTemplate.hasSplitName()) {
                    for (String singleTagName : StringUtil.splitAndTrim(tagName, tagTemplate.getSplitName())) {
                        if (empty(singleTagName)) continue;
                        tags.add(buildTag(handlebars, EMPTY_CTX, tagTemplate, tagType, singleTagName));
                    }
                } else {
                    tags.add(buildTag(handlebars, EMPTY_CTX, tagTemplate, tagType, tagName));
                }
            }
            nexus.setTags(new NexusTags(tags));
        }

        return nexus;
    }

    private NexusTag buildTag(Handlebars handlebars, Map<String, Object> ctx, NexusTagTemplate tagTemplate, String tagType, String tagName) {
        final NexusTag tag = new NexusTag();
        tag.setTagType(tagType);
        tag.setTagName(tagName);
        if (tagTemplate.hasSchemaValues()) {
            final List<TagSchemaValue> values = new ArrayList<>();
            for (TagSchemaValue decorator : tagTemplate.getValues()) {
                final String decoratorName = HandlebarsUtil.apply(handlebars, decorator.getField(), ctx);
                final String decoratorValue = HandlebarsUtil.apply(handlebars, decorator.getValue(), ctx);
                if (!empty(decoratorName) && !empty(decoratorValue)) {
                    values.add(new TagSchemaValue(decoratorName, decoratorValue));
                }
            }
            tag.setValues(values.toArray(new TagSchemaValue[values.size()]));
        }
        return tag;
    }

    private Set<String> findXpaths(Feed feed) {
        final Set<String> xpaths = new HashSet<>();
        final Handlebars handlebars = getXpathCounterHandlebars(xpaths);
        HandlebarsUtil.apply(handlebars, json(feed.getNexus()), EMPTY_CTX);
        if (feed.hasMatch()) HandlebarsUtil.apply(handlebars, feed.getMatch(), EMPTY_CTX);
        return xpaths;
    }

    public Handlebars getXpathCounterHandlebars(final Set<String> xpaths) {
        final Handlebars hb = new Handlebars(new HandlebarsUtil("histori-rss-xpath-counter"));
        hb.registerHelper("xpath", new Helper<Object>() {
            public CharSequence apply(Object src, Options options) {
                if (empty(src)) return "";
                synchronized (xpaths) { xpaths.add(src.toString().replace("\\'", "'")); }
                return "";
            }
        });
        hb.registerHelper("xpath-match", new Helper<Object>() {
            public CharSequence apply(Object src, Options options) {
                if (empty(src)) return "";
                synchronized (xpaths) { xpaths.add(src.toString().replace("\\'", "'")); }
                return "";
            }
        });
        return hb;
    }

    public Handlebars getXpathHandlebars(final Map<String, List<Node>> xpathMatches) {
        final Handlebars hb = new Handlebars(new HandlebarsUtil("histori-rss-xpath"));
        hb.registerHelper("xpath", new Helper<Object>() {
            public CharSequence apply(Object src, Options options) {
                if (empty(src)) return "";
                final String path = src.toString();
                final List<Node> nodes = xpathMatches.get(path);
                switch (nodes.size()) {
                    case 0: return "";
                    case 1: return nodes.iterator().next().getTextContent();
                    default: return die("xpath helper: multiple matches for path: "+ path);
                }
            }
        });
        hb.registerHelper("xpath-match", new Helper<Object>() {
            public CharSequence apply(Object src, Options options) {
                if (empty(src)) return "";
                final String path = src.toString();
                final String arg = options.param(0);
                final List<Node> nodes = xpathMatches.get(path);
                switch (nodes.size()) {
                    case 0: return "false";
                    case 1: return ""+nodes.iterator().next().getTextContent().matches(arg);
                    default: return die("xpath-match helper: multiple matches for path: "+ path);
                }
            }
        });
        return hb;
    }

}
