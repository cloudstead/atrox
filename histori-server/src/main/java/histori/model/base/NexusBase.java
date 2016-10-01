package histori.model.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.CanonicalEntity;
import histori.model.NexusTag;
import histori.model.NexusView;
import histori.model.SocialEntity;
import histori.model.support.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.math.Cardinal;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.dao.sql.SQLFieldTransformer;
import org.geojson.GeoJsonObject;
import org.geojson.Geometry;
import org.geojson.LngLatAlt;
import org.geojson.Point;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.math.BigInteger;
import java.util.*;

import static histori.ApiConstants.GEOJSON_MAXLEN;
import static histori.ApiConstants.NAME_MAXLEN;
import static histori.model.TagType.EVENT_TYPE;
import static histori.model.base.NexusTags.JSONB_TYPE;
import static java.math.BigInteger.ZERO;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.util.string.StringUtil.hasScripting;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@MappedSuperclass @Accessors(chain=true) @ToString(of="name") @Slf4j
public abstract class NexusBase extends SocialEntity implements NexusView, Comparator<NexusBase> {

    @SuppressWarnings("Duplicates")
    public static Comparator<NexusView> comparator (SearchSortOrder sort) {
        switch (sort) {
            case newest:              return NCompare_newest.instance;
            case oldest:              return NCompare_oldest.instance;
            case up_vote:             return NCompare_up_vote.instance;
            case down_vote:           return NCompare_down_vote.instance;
            case vote_count:          return NCompare_vote_count.instance;
            case vote_tally: default: return NCompare_vote_tally.instance;
        }
    }

    @Override public void beforeCreate() {
        initUuid();
        getBounds();  // ensure bounds are set
    }

    @Getter @Setter private boolean authoritative;

    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Getter @Setter private String canonicalName;

    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Getter private String name;
    public boolean hasName() { return !empty(name); }

    public NexusBase setName(String name) {
        this.name = name;
        this.canonicalName = CanonicalEntity.canonicalize(name);
        return this;
    }

    @Transient @Getter @Setter private String displayName;

    @Column(length=100)
    @Size(max=100, message="err.book.length")
    @Getter @Setter private String book;

    // Which event_type tag is "primary" (if there is only 1 then it is here by default)
    @Column(length=NAME_MAXLEN)
    @Size(max=NAME_MAXLEN, message="err.nexusType.length")
    @Getter @Setter private String nexusType;
    public boolean hasNexusType () { return !empty(nexusType); }

    @Embedded @Getter private TimeRange timeRange;

    public NexusBase setTimeRange(TimeRange range) {
        if (range == null || !range.hasStart() || range.getStartPoint().getInstant().equals(ZERO)) {
            log.warn("setTimeRange: not overwriting self with empty value");
        } else {
            this.timeRange = range;
        }
        return this;
    }

    public boolean hasRange () { return timeRange != null && timeRange.hasStart(); }

    @Size(max=GEOJSON_MAXLEN, message="err.geolocation.tooLong")
    @Column(length=GEOJSON_MAXLEN, nullable=false)
    @JsonIgnore @Getter private String geoJson;

    public void setGeoJson(String geoJson) {
        if (empty(geoJson) || emptyGeo(geoJson(geoJson))) {
            log.warn("setGeoJson: not overwriting self with empty value");
            return;
        }
        this.geoJson = geoJson;
    }

    private static boolean emptyGeo(GeoJsonObject geo) {
        if (geo == null) return true;
        if (geo instanceof Point) {
            final Point p = (Point) geo;
            LngLatAlt coordinates = p.getCoordinates();
            if (coordinates == null || (coordinates.getLongitude() == 0 && coordinates.getLatitude() == 0)) return true;
        } else {
            die("emptyGeo: not supported: "+geo.getClass().getName());
        }
        return false;
    }

    @Embedded @Setter private GeoBounds bounds = null;
    public GeoBounds getBounds() {
        if (bounds == null) {
            // recalculate bounding coordinates
            final GeoJsonObject geo = getGeo();
            if (geo instanceof Point) {
                final Point p = (Point) geo;
                bounds = new GeoBounds(p.getCoordinates().getLatitude(), p.getCoordinates().getLatitude(),
                        p.getCoordinates().getLongitude(), p.getCoordinates().getLongitude());
            } else if (geo instanceof Geometry) {
                bounds =  GeoBounds.blank();
                final Geometry g = (Geometry) geo;
                for (Object coordinates : g.getCoordinates()) {
                    if (coordinates instanceof LngLatAlt) {
                        final LngLatAlt coord = (LngLatAlt) coordinates;
                        bounds.expandToFit(coord.getLatitude(), coord.getLongitude());
                    }
                    if (coordinates instanceof List) {
                        for (LngLatAlt coord : (List<LngLatAlt>) coordinates) {
                            bounds.expandToFit(coord.getLatitude(), coord.getLongitude());
                        }
                    }
                }
            }
        }
        return bounds;
    }

    @Transient private GeoJsonObject geo = null;
    public GeoJsonObject getGeo() {
        if (geo == null) geo = geoJson(getGeoJson());
        return geo;
    }

    protected GeoJsonObject geoJson(String geoJson) { return fromJsonOrDie(geoJson, GeoJsonObject.class); }

    public void setGeo(GeoJsonObject geo) {
        this.geo = geo;
        this.geoJson = toJsonOrDie(geo);
    }

    public void prepareForSave() {
        // check name, tags and markdown for nefarious scripting
        if (containsScripting()) throw invalidEx("err.scripting");

        // ensure event type tag exists
        if (hasNexusType() && (!hasTags() || getTags().getFirstEventType() == null)) getTags().addTag(getNexusType(), EVENT_TYPE);

        // ensure all tags have uuids, refresh tagsJson
        if (hasTags()) {
            for (NexusTag tag : getTags()) if (!tag.hasUuid()) tag.initUuid();
        }

        if (timeRange == null) throw invalidEx("err.timeRange.empty", "Time range cannot be empty");
        if (!timeRange.hasStart()) throw invalidEx("err.timeRange.start.empty", "Start date cannot be empty");
        timeRange.getStartPoint().initInstant();
        if (timeRange.hasEnd()) {
            timeRange.getEndPoint().initInstant();
        } else {
            timeRange.setEndPoint(null);
        }
    }

    public boolean containsScripting() {
        if (hasScripting(getName())) return true;
        if (hasScripting(getMarkdown())) return true;
        if (hasTags()) {
            for (NexusTag tag : getTags()) {
                if (hasScripting(tag.getCanonicalName()) || hasScripting(tag.getTagName())) return true;
                if (tag.hasSchemaValues()) {
                    for (Map.Entry<String, TreeSet<String>> entry : tag.getSchemaValueMap().allEntrySets()) {
                        if (hasScripting(entry.getKey())) return true;
                        for (String value : entry.getValue()) if (hasScripting(value)) return true;
                    }
                }
            }
        }
        return false;
    }

    private static final String[] ID_FIELDS = {"owner", "canonicalName"};
    @Override public String[] getIdentifiers() { return new String [] { getOwner(), getCanonicalName() }; }
    @Override public String[] getIdentifierFields() { return ID_FIELDS; }

    public void setTimeRange(String startDate, String endDate) { setTimeRange(new TimeRange(startDate, endDate)); }

    @Type(type=JSONB_TYPE)
    @Getter @Setter private NexusTags tags = new NexusTags();
    public boolean hasTags () { return !empty(tags); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        final NexusBase nexusBase = (NexusBase) o;

        if (authoritative != nexusBase.authoritative) return false;
        if (!canonicalName.equals(nexusBase.canonicalName)) return false;
        if (!timeRange.equals(nexusBase.timeRange)) return false;
        if (!geoJson.equals(nexusBase.geoJson)) return false;

        if (nexusType == null && nexusBase.nexusType != null) return false;
        else if (nexusType != null && nexusBase.nexusType == null) return false;
        else if (nexusType != null && !nexusType.equals(nexusBase.nexusType)) return false;

        final int thisHash = getTags().hashCode();
        final int otherHash = nexusBase.getTags().hashCode();
        return thisHash == otherHash;
    }

    @Override public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Boolean.valueOf(authoritative).hashCode();
        result = 31 * result + canonicalName.hashCode();
        result = 31 * result + timeRange.hashCode();
        result = 31 * result + getGeoJson().hashCode();
        if (hasTags()) result = 31 * result + getTags().hashCode();
        return result;
    }

    @Override public int compare(NexusBase o1, NexusBase o2) {
        return o1.getCanonicalName().compareTo(o2.getCanonicalName());
    }

    public void scrubShortTagNames() {
        if (hasTags()) {
            for (Iterator<NexusTag> iter = getTags().iterator(); iter.hasNext();) {
                NexusTag tag = iter.next();
                if (tag.getTagName().isEmpty()) iter.remove();
            }
            setTags(getTags());
        }
    }

    private static abstract class NCompare implements Comparator<NexusView> {
        @Override public int compare(NexusView o1, NexusView o2) {
            long v1 = val(o1);
            long v2 = val(o2);
            final int diff = reverse() ? Long.compare(v1, v2) : Long.compare(v2, v1);
            return diff != 0 ? diff : secondaryCompare(o1, o2);
        }
        protected abstract long val(NexusView view);
        protected boolean reverse () { return false; }
        protected int secondaryCompare(NexusView o1, NexusView o2) { return Long.compare(o1.getCtime(), o2.getCtime()); }
    }

    private static class NCompare_newest extends NCompare {
        static final NCompare_newest instance = new NCompare_newest();
        @Override protected long val(NexusView view) { return view.getCtime(); }
    }
    private static class NCompare_oldest extends NCompare {
        static final NCompare_oldest instance = new NCompare_oldest();
        @Override protected long val(NexusView view) { return view.getCtime(); }
        @Override protected boolean reverse() { return true; }
    }
    private static class NCompare_up_vote extends NCompare {
        static final NCompare_up_vote instance = new NCompare_up_vote();
        @Override protected long val(NexusView view) { return view.hasVotes() ? view.getVotes().getUpVotes() : 0; }
    }
    private static class NCompare_down_vote extends NCompare {
        static final NCompare_down_vote instance = new NCompare_down_vote();
        @Override protected long val(NexusView view) { return view.hasVotes() ? view.getVotes().getDownVotes() : 0; }
    }
    private static class NCompare_vote_count extends NCompare {
        static final NCompare_vote_count instance = new NCompare_vote_count();
        @Override protected long val(NexusView view) { return view.hasVotes() ? view.getVotes().getVoteCount() : 0; }
    }
    private static class NCompare_vote_tally extends NCompare {
        static final NCompare_vote_tally instance = new NCompare_vote_tally();
        @Override protected long val(NexusView view) { return view.hasVotes() ? view.getVotes().getTally() : 0; }
    }

    @JsonIgnore @Transient
    @Getter(lazy=true) private final Map<String, SQLFieldTransformer> sQLFieldTransformers = initSQLFieldTransformers();
    private Map<String, SQLFieldTransformer> initSQLFieldTransformers() {
        final Map<String, SQLFieldTransformer> map = super.getSQLFieldTransformers();
        map.put("visibility", EntityVisibility.TRANSFORMER);
        map.put("north", NORTH_XFORM);
        map.put("south", SOUTH_XFORM);
        map.put("east", EAST_XFORM);
        map.put("west", WEST_XFORM);
        map.put("start_instant", START_INSTANT_XFORM);
        map.put("start_year", START_YEAR_XFORM);
        map.put("start_month", START_MONTH_XFORM);
        map.put("start_day", START_DAY_XFORM);
        map.put("start_hour", START_HOUR_XFORM);
        map.put("start_minute", START_MINUTE_XFORM);
        map.put("start_second", START_SECOND_XFORM);
        map.put("end_instant", END_INSTANT_XFORM);
        map.put("end_year", END_YEAR_XFORM);
        map.put("end_month", END_MONTH_XFORM);
        map.put("end_day", END_DAY_XFORM);
        map.put("end_hour", END_HOUR_XFORM);
        map.put("end_minute", END_MINUTE_XFORM);
        map.put("end_second", END_SECOND_XFORM);
        map.put("tags", TAGS_XFORM);
        return map;
    }

    @AllArgsConstructor @Slf4j
    public static class BoundsTransformer implements SQLFieldTransformer {
        private Cardinal direction;
        @Override public Object sqlToObject(Object object, Object input) {
            final NexusBase nexus = (NexusBase) object;
            GeoBounds bounds = nexus.getBounds();
            if (bounds == null) {
                bounds = new GeoBounds();
                nexus.setBounds(bounds);
            }
            final Double value = ((Number) input).doubleValue();
            switch (direction) {
                case north: bounds.setNorth(value); break;
                case south: bounds.setSouth(value); break;
                case east: bounds.setEast(value); break;
                case west: bounds.setWest(value); break;
                default: log.warn("unknown cardinality: "+direction);
            }
            return null;
        }
    }
    public static final SQLFieldTransformer NORTH_XFORM = new BoundsTransformer(Cardinal.north);
    public static final SQLFieldTransformer SOUTH_XFORM = new BoundsTransformer(Cardinal.south);
    public static final SQLFieldTransformer EAST_XFORM = new BoundsTransformer(Cardinal.east);
    public static final SQLFieldTransformer WEST_XFORM = new BoundsTransformer(Cardinal.west);

    @AllArgsConstructor @Slf4j
    public static class TimeRangeTransformer implements SQLFieldTransformer {
        private String startOrEnd;
        private String field;
        @Override public Object sqlToObject(Object object, Object input) {
            final NexusBase nexus = (NexusBase) object;
            TimeRange range = nexus.getTimeRange();
            if (range == null) {
                range = new TimeRange();
                nexus.setTimeRange(range);
            }
            TimePoint point = (startOrEnd.equals("start") ? range.getOrCreateStartPoint() : range.getOrCreateEndPoint());
            switch (field) {
                case "instant": point.setInstant(new BigInteger(input.toString())); break;
                case "year": point.setYear(((Number) input).longValue()); break;
                default: ReflectionUtil.set(point, field, ((Number) input).byteValue());
            }
            return null;
        }
    }
    public static final SQLFieldTransformer START_INSTANT_XFORM = new TimeRangeTransformer("start", "instant");
    public static final SQLFieldTransformer START_YEAR_XFORM = new TimeRangeTransformer("start", "year");
    public static final SQLFieldTransformer START_MONTH_XFORM = new TimeRangeTransformer("start", "month");
    public static final SQLFieldTransformer START_DAY_XFORM = new TimeRangeTransformer("start", "day");
    public static final SQLFieldTransformer START_HOUR_XFORM = new TimeRangeTransformer("start", "hour");
    public static final SQLFieldTransformer START_MINUTE_XFORM = new TimeRangeTransformer("start", "minute");
    public static final SQLFieldTransformer START_SECOND_XFORM = new TimeRangeTransformer("start", "second");

    public static final SQLFieldTransformer END_INSTANT_XFORM = new TimeRangeTransformer("end", "instant");
    public static final SQLFieldTransformer END_YEAR_XFORM = new TimeRangeTransformer("end", "year");
    public static final SQLFieldTransformer END_MONTH_XFORM = new TimeRangeTransformer("end", "month");
    public static final SQLFieldTransformer END_DAY_XFORM = new TimeRangeTransformer("end", "day");
    public static final SQLFieldTransformer END_HOUR_XFORM = new TimeRangeTransformer("end", "hour");
    public static final SQLFieldTransformer END_MINUTE_XFORM = new TimeRangeTransformer("end", "minute");
    public static final SQLFieldTransformer END_SECOND_XFORM = new TimeRangeTransformer("end", "second");

    public static final SQLFieldTransformer TAGS_XFORM = new SQLFieldTransformer() {
        @Override public Object sqlToObject(Object object, Object input) {
            return JsonUtil.fromJsonOrDie(input.toString(), NexusTags.class);
        }
    };
}
