package histori.model.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.NexusTag;
import histori.model.SocialEntity;
import histori.model.support.GeoBounds;
import histori.model.support.TimeRange;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.MapBuilder;
import org.geojson.GeoJsonObject;
import org.geojson.Geometry;
import org.geojson.LngLatAlt;
import org.geojson.Point;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static histori.ApiConstants.GEOJSON_MAXLEN;
import static histori.ApiConstants.NAME_MAXLEN;
import static histori.model.CanonicalEntity.canonicalize;
import static histori.model.TagType.EVENT_TYPE;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@MappedSuperclass @Accessors(chain=true) @ToString(of="name")
public class NexusBase extends SocialEntity {

    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Getter @Setter private String name;
    public boolean hasName() { return !empty(name); }

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

    @Embedded @Getter @Setter private GeoBounds bounds;

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
        if (timeRange == null) throw invalidEx("err.timeRange.empty", "Time range cannot be empty");
        if (!timeRange.hasStart()) throw invalidEx("err.timeRange.start.empty", "Start date cannot be empty");
        timeRange.getStartPoint().initInstant();
        if (timeRange.hasEnd()) timeRange.getEndPoint().initInstant();

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

    private static final String[] ID_FIELDS = {"owner", "name"};
    @Override public String[] getIdentifiers() { return new String [] { getOwner(), getName() }; }
    @Override public String[] getIdentifierFields() { return ID_FIELDS; }

    public void setTimeRange(String startDate, String endDate) { setTimeRange(new TimeRange(startDate, endDate)); }

    @Transient @Getter @Setter private List<NexusTag> tags;
    public boolean hasTags () { return !empty(tags); }

    public NexusBase addTag (NexusTag tag) {
        if (tags == null) tags = new ArrayList<>();
        for (NexusTag existing : tags) {
            if (tag.isSameTag(existing)) return this;
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
        final NexusTag tag = (NexusTag) new NexusTag().setTagName(name);
        if (!empty(tagFields)) {
            for (Map.Entry<String, String> field : tagFields.entrySet()) {
                tag.setValue(field.getKey(), field.getValue());
            }
        }
        if (!empty(tagType)) tag.setTagType(tagType);
        addTag(tag);
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
            for (NexusTag tag : tags) if (tag.getTagType().equalsIgnoreCase(EVENT_TYPE)) return tag.getTagName();
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
}
