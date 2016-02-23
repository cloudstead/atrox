package histori.model.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.NexusTag;
import histori.model.SocialEntity;
import histori.model.support.TimeRange;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.geojson.GeoJsonObject;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static histori.ApiConstants.GEOJSON_MAXLEN;
import static histori.ApiConstants.NAME_MAXLEN;
import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@MappedSuperclass @Accessors(chain=true) @ToString(of="name")
public class NexusBase extends SocialEntity {

    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @Size(min=2, max=NAME_MAXLEN, message="err.name.length")
    @Getter @Setter private String name;

    @Size(max=GEOJSON_MAXLEN, message="err.geolocation.tooLong")
    @Column(length=GEOJSON_MAXLEN, nullable=false)
    @JsonIgnore @Getter @Setter private String geoJson;

    @Transient private GeoJsonObject geo = null;

    public GeoJsonObject getGeo() {
        if (geo == null) geo = fromJsonOrDie(geoJson, GeoJsonObject.class);
        return geo;
    }

    public void setGeo(GeoJsonObject geo) {
        this.geo = geo;
        this.geoJson = toJsonOrDie(geo);
    }

    @Embedded @Getter @Setter private TimeRange timeRange;

    public void initTimeInstants() {
        if (timeRange == null) throw invalidEx("err.timeRange.empty");
        if (!timeRange.hasStart()) throw invalidEx("err.timeRange.start.empty");
        timeRange.getStartPoint().initInstant();
        if (timeRange.hasEnd()) timeRange.getEndPoint().initInstant();
    }

    private static final String[] ID_FIELDS = {"owner", "name"};
    @Override public String[] getIdentifiers() { return new String [] { getOwner(), getName() }; }
    @Override public String[] getIdentifierFields() { return ID_FIELDS; }

    public void setTimeRange(String startDate, String endDate) { setTimeRange(new TimeRange(startDate, endDate)); }

    @Transient @Getter @Setter private List<NexusTag> tags;
    public boolean hasTags () { return !empty(tags); }

    public NexusBase addTag (String name) { return addTag(name, null, null); }

    public NexusBase addTag (String name, String tagType) { return addTag(name, tagType, null); }

    public NexusBase addTag (String name, Map<String, String> tagFields) { return addTag(name, null, tagFields); }

    public NexusBase addTag (String name, String tagType, Map<String, String> tagFields) {
        if (tags == null) tags = new ArrayList<>();
        final NexusTag tag = (NexusTag) new NexusTag().setTagName(name);
        if (!empty(tagFields)) tag.setSchemaValues(toJsonOrDie(tagFields));
        if (!empty(tagType)) tag.setTagType(tagType);
        tags.add(tag);
        return this;
    }

    public NexusTag getTag(String name) {
        if (empty(tags)) return null;
        final String canonical = canonicalize(name);
        for (NexusTag tag : tags) {
            if (canonicalize(tag.getTagName()).equals(canonical)) return tag;
        }
        return null;
    }

    public NexusTag getTag(String tagType, String name) {
        if (empty(tags)) return null;
        final String canonical = canonicalize(name);
        for (NexusTag tag : tags) {
            if (tag.getTagType().equalsIgnoreCase(tagType) && canonicalize(tag.getTagName()).equals(canonical)) return tag;
        }
        return null;
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

}
