package histori.feed;

import com.github.jknack.handlebars.Handlebars;
import edu.emory.mathcs.backport.java.util.Arrays;
import histori.dao.AccountDAO;
import histori.dao.FeedDAO;
import histori.dao.NexusDAO;
import histori.model.*;
import histori.model.base.NexusTags;
import histori.model.support.TimeRange;
import histori.model.tag_schema.TagSchemaValue;
import histori.model.template.NexusTagTemplate;
import histori.model.template.NexusTemplate;
import histori.server.HistoriConfiguration;
import histori.wiki.WikiDateFormat;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.MapBuilder;
import org.cobbzilla.util.daemon.SimpleDaemon;
import org.cobbzilla.util.handlebars.HandlebarsUtil;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static histori.model.tag_schema.TagSchemaField.LAST_ACCESSED;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@Service @Slf4j
public class FeedService extends SimpleDaemon {

    public static final long FEED_CACHE_EXPIRATION = TimeUnit.HOURS.toMillis(1);
    public static final long FEED_POLL_INTERVAL = TimeUnit.HOURS.toMillis(1);
    public static final Map<String, Object> EMPTY_CTX = Collections.emptyMap();

    public FeedService() { start(); }

    @Autowired private HistoriConfiguration configuration;
    @Autowired private AccountDAO accountDAO;
    @Autowired private FeedDAO feedDAO;
    @Autowired private NexusDAO nexusDAO;
    @Autowired private RedisService redisService;

    @Getter(lazy=true) private final RedisService feedCache = initFeedCache();
    private RedisService initFeedCache() { return redisService.prefixNamespace(FeedService.class.getName()+":feeds", null); }

    public List<Nexus> readAndSave(Feed feed) { return read(feed, true); }
    public List<Nexus> read(Feed feed) { return read(feed, false); }

    public List<Nexus> read(Feed feed, boolean save) {
        List<Nexus> items;
        final String nexusJson = getFeedCache().get(feed.getSource());
        final Account feedOwner = accountDAO.findByUuid(feed.getOwner());
        if (nexusJson == null) {
            items = getFeedReader(feed.getReader()).read(feed);
            if (feed.isActive()) getFeedCache().set(feed.getSource(), json(items), "EX", FEED_CACHE_EXPIRATION);
        } else {
            items = Arrays.asList(json(nexusJson, Nexus[].class));
        }
        if (feed.isActive() && save) {
            for (int i=0; i<items.size(); i++) {
                final Nexus nexus = items.get(i);
                nexus.setFeed(feed.getUuid());
                nexus.getTags().addTag(feed.getSource(), "citation", MapBuilder.build(LAST_ACCESSED, ""+now()));
                items.set(i, nexusDAO.createOrUpdateNexus(feedOwner, nexus));
            }
        }
        return items;
    }

    private final Map<String, FeedReader> readerCache = new ConcurrentHashMap<>();
    private FeedReader getFeedReader(String readerClass) {
        FeedReader reader = readerCache.get(readerClass);
        if (reader == null) {
            reader = instantiate(readerClass);
            reader = configuration.autowire(reader);
            readerCache.put(readerClass, reader);
        }
        return reader;
    }

    public static Nexus processItem(Feed feed, Handlebars handlebars) {

        final NexusTemplate nexusTemplate = feed.getNexus();
        final Nexus nexus = new Nexus();

        nexus.setBook(feed.getBook());
        nexus.setName(subst(handlebars, nexusTemplate.getName()));

        final TimeRange timeRange = new TimeRange();
        final String startString = subst(handlebars, nexusTemplate.getTimeRange().getStart());
        timeRange.setStartPoint(WikiDateFormat.parseStart(startString));
        final String endString = subst(handlebars, nexusTemplate.getTimeRange().getStart());
        timeRange.setEndPoint(WikiDateFormat.parseStart(endString));
        nexus.setTimeRange(timeRange);

        final String geoJson = subst(handlebars, nexusTemplate.getGeoJson());
        nexus.setGeoJson(geoJson);

        if (nexusTemplate.hasNexusType()) {
            nexus.setNexusType(subst(handlebars, nexusTemplate.getNexusType()));
        }
        if (nexusTemplate.hasMarkdown()) {
            nexus.setMarkdown(subst(handlebars, nexusTemplate.getMarkdown()));
        }
        if (nexusTemplate.hasTags()) {
            final List<NexusTag> tags = new ArrayList<>();
            for (NexusTagTemplate tagTemplate : nexusTemplate.getTags()) {

                final String tagName = subst(handlebars, tagTemplate.getTagName());
                final String tagType = subst(handlebars, tagTemplate.getTagType());
                if (empty(tagType) || empty(tagName)) continue;

                if (tagTemplate.hasSplitName()) {
                    for (String singleTagName : StringUtil.splitAndTrim(tagName, tagTemplate.getSplitName())) {
                        if (empty(singleTagName)) continue;
                        final NexusTag tag = buildTag(handlebars, EMPTY_CTX, tagTemplate, tagType, singleTagName);
                        if (tag != null) tags.add(tag);
                    }
                } else {
                    final NexusTag tag = buildTag(handlebars, EMPTY_CTX, tagTemplate, tagType, tagName);
                    if (tag != null) tags.add(tag);
                }
            }
            nexus.setTags(new NexusTags(tags));
        }

        return nexus;
    }

    public static String subst(Handlebars handlebars, String name) { return subst(handlebars, name, EMPTY_CTX); }
    public static String subst(Handlebars handlebars, String name, Map<String, Object> ctx) {
        return HandlebarsUtil.apply(handlebars, name, ctx);
    }

    public static NexusTag buildTag(Handlebars handlebars, Map<String, Object> ctx, NexusTagTemplate tagTemplate, String tagType, String tagName) {
        final NexusTag tag = new NexusTag();
        tag.setTagType(tagType);
        tag.setTagName(tagName);
        if (tagTemplate.hasSchemaValues()) {
            final List<TagSchemaValue> values = new ArrayList<>();
            for (TagSchemaValue decorator : tagTemplate.getValues()) {
                final String decoratorName = subst(handlebars, decorator.getField(), ctx);
                final String decoratorValue = subst(handlebars, decorator.getValue(), ctx);
                if (!empty(decoratorName) && !empty(decoratorValue)) {
                    values.add(new TagSchemaValue(decoratorName, decoratorValue));
                }
            }
            if (values.isEmpty()) return null;
            tag.setValues(values.toArray(new TagSchemaValue[values.size()]));
        }
        return tag;
    }

    @Override protected long getStartupDelay() { return TimeUnit.MINUTES.toMillis(5); }

    private static final Object processLock = new Object();
    @Override protected long getSleepTime() { return FEED_POLL_INTERVAL; }

    @Override protected void process() {
        synchronized (processLock) {
            for (Feed feed : feedDAO.findActive()) {
                readAndSave(feed);
            }
        }
    }
}
