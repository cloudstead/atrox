package histori.model.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.emory.mathcs.backport.java.util.Arrays;
import histori.model.CanonicalEntity;
import histori.model.NexusTag;
import histori.model.NexusView;
import histori.model.SocialEntity;
import histori.model.support.GeoBounds;
import histori.model.support.SearchSortOrder;
import histori.model.support.TimeRange;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.collection.MapBuilder;
import org.cobbzilla.util.collection.mappy.MappySortedSet;
import org.geojson.GeoJsonObject;
import org.geojson.Geometry;
import org.geojson.LngLatAlt;
import org.geojson.Point;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.util.*;

import static histori.ApiConstants.GEOJSON_MAXLEN;
import static histori.ApiConstants.NAME_MAXLEN;
import static histori.model.CanonicalEntity.canonicalize;
import static histori.model.TagType.EVENT_TYPE;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.util.system.Bytes.MB;
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

    @Transient public String getDisplayName () { return name == null ? null : name.replace("_", " ").trim(); }
    public void setDisplayName (String name) {/* noop */}

    // Which event_type tag is "primary" (if there is only 1 then it is here by default)
    @Column(length=NAME_MAXLEN)
    @Size(max=NAME_MAXLEN, message="err.nexusType.length")
    @Getter @Setter private String nexusType;
    public boolean hasNexusType () { return !empty(nexusType); }

    @Embedded @Getter @Setter private TimeRange timeRange;
    public boolean hasRange () { return timeRange != null && timeRange.hasStart(); }

    @Size(max=GEOJSON_MAXLEN, message="err.geolocation.tooLong")
    @Column(length=GEOJSON_MAXLEN, nullable=false)
    @JsonIgnore @Getter @Setter private String geoJson;

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
        if (geo == null) geo = fromJsonOrDie(geoJson, GeoJsonObject.class);
        return geo;
    }
    public void setGeo(GeoJsonObject geo) {
        this.geo = geo;
        this.geoJson = toJsonOrDie(geo);
    }

    public void prepareForSave() {

        // ensure event type tag exists
        if (hasNexusType() && getFirstEventType() == null) addTag(getNexusType(), EVENT_TYPE);

        // ensure all tags have uuids, refresh tagsJson
        if (hasTags()) {
            for (NexusTag tag : getTags()) if (!tag.hasUuid()) tag.initUuid();
            setTagsJson(toJsonOrDie(getTags()));
        }

        if (timeRange == null) throw invalidEx("err.timeRange.empty", "Time range cannot be empty");
        if (!timeRange.hasStart()) throw invalidEx("err.timeRange.start.empty", "Start date cannot be empty");
        timeRange.getStartPoint().initInstant();
        if (timeRange.hasEnd()) timeRange.getEndPoint().initInstant();
    }

    private static final String[] ID_FIELDS = {"owner", "canonicalName"};
    @Override public String[] getIdentifiers() { return new String [] { getOwner(), getCanonicalName() }; }
    @Override public String[] getIdentifierFields() { return ID_FIELDS; }

    public void setTimeRange(String startDate, String endDate) { setTimeRange(new TimeRange(startDate, endDate)); }

    @Column(length=(int)(MB)) // 1 megabyte should be enough... famous last words, right? we can always expand, or go to a document store
    @JsonIgnore @Getter @Setter private String tagsJson;

    @Transient private List<NexusTag> tags = null;

    public List<NexusTag> getTags () {
        if (empty(tagsJson) && empty(tags)) return new ArrayList<>();
        if (tags == null) tags = Arrays.asList(fromJsonOrDie(tagsJson, NexusTag[].class));
        return tags;
    }

    public NexusBase setTags (List<NexusTag> tags) {
        this.tags = tags;
        this.tagsJson = toJsonOrDie(tags);
        return this;
    }

    public boolean hasTags () { return !empty(tags) && !empty(tagsJson); }

    public NexusBase addTag (NexusTag tag) {
        if (tags == null) tags = new ArrayList<>();
        for (NexusTag existing : tags) {
            if (tag.isSameTag(existing)) return this; // re-adding an identical tag is OK, just a noop
        }
        tags.add(tag);
        return this;
    }

    public NexusBase addTag (String name) { return addTag(name, null, null); }

    public NexusBase addTag (String name, String tagType) { return addTag(name, tagType, null); }

    public NexusBase addTag (String name, Map<String, String> tagFields) { return addTag(name, null, tagFields); }

    public NexusBase addTag (String name, String tagType, String field, String value) {
        return addTag(name, tagType, MapBuilder.build(field, value));
    }

    public NexusBase addTag (String name, String tagType, Map<String, String> tagFields) {
        if (tags == null) tags = new ArrayList<>();
        final NexusTag tag = new NexusTag().setTagName(name);
        if (!empty(tagFields)) {
            for (Map.Entry<String, String> field : tagFields.entrySet()) {
                tag.setValue(field.getKey(), field.getValue());
            }
        }
        if (!empty(tagType)) tag.setTagType(tagType);
        addTag(tag);
        this.tagsJson = toJsonOrDie(tags);
        return this;
    }

    public List<NexusTag> getTag(String name) {
        final List<NexusTag> found = new ArrayList<>();
        if (!empty(tags)) {
            final String canonical = canonicalize(name);
            for (NexusTag tag : tags) {
                if (canonicalize(tag.getTagName()).equals(canonical)) found.add(tag);
            }
        }
        return found;
    }

    public List<NexusTag> getTag(String tagType, String name) {
        final List<NexusTag> found = new ArrayList<>();
        if (!empty(tags)) {
            final String canonical = canonicalize(name);
            for (NexusTag tag : tags) {
                if (tag.getTagType().equalsIgnoreCase(tagType) && canonicalize(tag.getTagName()).equals(canonical)) {
                    found.add(tag);
                }
            }
        }
        return found;
    }

    public boolean hasTag(String name) {
        if (empty(tags) || empty(name)) return false;
        final String canonical = canonicalize(name);
        for (NexusTag tag : tags) {
            if (canonicalize(tag.getTagName()).equals(canonical)) return true;
        }
        return false;
    }

    public boolean hasTag(String name, String type) {
        if (empty(tags) || empty(name)) return false;
        final String canonical = canonicalize(name);
        for (NexusTag tag : tags) {
            if (type != null && (!tag.hasTagType() || !tag.getTagType().equals(type))) continue;
            if (canonicalize(tag.getTagName()).equals(canonical)) return true;
        }
        return false;
    }

    @JsonIgnore @Transient public int getTagCount() { return empty(tags) ? 0 : tags.size(); }

    public boolean hasExactTag(NexusTag match) {
        if (!hasTags()) return false;
        for (NexusTag tag : getTags()) {
            if (!tag.getTagName().equalsIgnoreCase(match.getTagName())) continue;
            if (!tag.getTagType().equalsIgnoreCase(match.getTagType())) continue;

            if (!tag.hasSchemaValues()) {
                if (match.hasSchemaValues()) continue;
                return true;
            }
            if (!match.hasSchemaValues()) continue;

            if (!tag.getSchemaValueMap().equals(match.getSchemaValueMap())) continue;

            return true;
        }
        return false;
    }

    public NexusTag getFirstTag(String name) {
        final List<NexusTag> found = getTag(name);
        return empty(found) ? null : found.get(0);
    }

    public List<NexusTag> getTagsByType(String type) {
        final List<NexusTag> found = new ArrayList<>();
        if (hasTags()) {
            for (NexusTag tag : tags) if (tag.getTagType().equalsIgnoreCase(type)) found.add(tag);
        }
        return found;
    }

    @JsonIgnore @Transient public String getFirstEventType () {
        if (hasTags()) {
            for (NexusTag tag : tags) if (tag.hasTagType() && tag.getTagType().equalsIgnoreCase(EVENT_TYPE)) return tag.getTagName();
        }
        return null;
    }

    public void removeTag(String uuid) {
        if (!hasTags()) return;
        for (Iterator<NexusTag> iter = tags.iterator(); iter.hasNext(); ) {
            final NexusTag tag = iter.next();
            if (tag.hasUuid() && tag.getUuid().equals(uuid)) iter.remove();
        }
    }

    @JsonIgnore @Transient public MappySortedSet<String, NexusTag> getTagMap () {
        final MappySortedSet<String, NexusTag> map = new MappySortedSet<>();
        for (NexusTag tag : getTags()) {
            map.put(tag.getCanonicalName(), tag);
        }
        return map;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        NexusBase nexusBase = (NexusBase) o;

        if (!canonicalName.equals(nexusBase.canonicalName)) return false;
        if (!timeRange.equals(nexusBase.timeRange)) return false;
        if (!geoJson.equals(nexusBase.geoJson)) return false;

        final MappySortedSet<String, NexusTag> tagMap = getTagMap();
        final MappySortedSet<String, NexusTag> nexusMap = nexusBase.getTagMap();
        return tagMap.equals(nexusMap);
    }

    @Override public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + canonicalName.hashCode();
        result = 31 * result + timeRange.hashCode();
        result = 31 * result + getGeoJson().hashCode();
        if (hasTags()) result = 31 * result + getTagMap().hashCode();
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
            return reverse() ? Long.compare(v1, v2) : Long.compare(v2, v1);
        }
        protected abstract long val(NexusView view);
        protected boolean reverse () { return false; }
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
}
