package histori.model.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.NexusTag;
import histori.model.SocialEntity;
import histori.model.tag_schema.TagSchemaValue;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.wizard.validation.HasValue;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.validation.constraints.Size;
import java.util.*;

import static histori.ApiConstants.NAME_MAXLEN;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;

@MappedSuperclass @Accessors(chain=true)
public class NexusTagBase extends SocialEntity {

    @Column(length=UUID_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String nexus;

    @Size(max=NAME_MAXLEN, message="err.tagName.tooLong")
    @Column(length=NAME_MAXLEN, nullable=false, updatable=false)
    @Getter @Setter private String tagName;

    @Transient public String getDisplayName () { return tagName.replace("_", " ").trim(); }
    public void setDisplayName (String name) {/* noop */}

    // denormalized
    @Size(max=NAME_MAXLEN, message="err.tagType.tooLong")
    @Column(length=NAME_MAXLEN)
    @Getter @Setter private String tagType;
    public boolean hasTagType () { return !empty(tagType); }

    @HasValue(message="err.schemaValue.empty")
    @Size(min=2, max=32000, message="err.schemaValues.tooLong")
    @Column(length=32000)
    @JsonIgnore @Getter @Setter private String schemaValues = "[]";
    public boolean hasSchemaValues () { return !empty(getValues()); }

    @Transient
    public TagSchemaValue[] getValues () { return empty(schemaValues) ? null : fromJsonOrDie(schemaValues, TagSchemaValue[].class); }
    public NexusTagBase setValues (TagSchemaValue[] values) { return setSchemaValues(empty(values) ? null : toJsonOrDie(values)); }

    public NexusTagBase setValue(String field, String value) {
        TagSchemaValue[] values = getValues();
        if (values == null) {
            values = new TagSchemaValue[] { new TagSchemaValue(field, value) };
        } else {
            values = ArrayUtil.append(values, new TagSchemaValue(field, value));
        }
        setValues(values);
        return this;
    }

    private static final String[] ID_FIELDS = new String[]{"uuid"};
    @Override public String[] getIdentifiers() { return new String[] {getUuid()}; }
    @Override public String[] getIdentifierFields() { return ID_FIELDS; }

    @Override public String toString() { return getTagType()+"/"+getTagName(); }

    @JsonIgnore @Transient public SchemaValueMap getSchemaValueMap() {
        final SchemaValueMap map = new SchemaValueMap();
        if (!hasSchemaValues()) return map;
        for (TagSchemaValue val : getValues()) {
            Set<String> found = map.get(val.getField());
            if (found == null) {
                found = new HashSet<>();
                map.put(val.getField(), found);
            }
            found.add(val.getValue());
        }
        return map;
    }

    public static List<NexusTag> filterByType(List<NexusTag> tags, String type) {
        final List<NexusTag> found = new ArrayList<>();
        if (!empty(tags)) {
            for (NexusTag tag : tags) {
                if (tag.getTagType().equalsIgnoreCase(type)) found.add(tag);
            }
        }
        return found;
    }

    public static boolean containsEventTypeTag(List<NexusTag> tags, String type) {
        if (!empty(tags)) {
            for (NexusTag tag : tags) if (tag.getTagType().equalsIgnoreCase(type)) return true;
        }
        return false;
    }

    public class SchemaValueMap extends HashMap<String, Set<String>> {
        @Override public boolean equals(Object o) {
            if (!(o instanceof SchemaValueMap)) return false;

            final SchemaValueMap m = (SchemaValueMap) o;

            if (m.size() != this.size()) return false;

            for (String field : this.keySet()) {
                final Set<String> values = this.get(field);
                final Set<String> mValues = m.get(field);

                if (mValues == null) return values == null;
                for (String val : values) if (!mValues.contains(val)) return false;
                for (String val : mValues) if (!values.contains(val)) return false;
            }

            for (String field : m.keySet()) {
                final Set<String> values = this.get(field);
                final Set<String> mValues = m.get(field);

                if (mValues == null) return values == null;
                for (String val : values) if (!mValues.contains(val)) return false;
                for (String val : mValues) if (!values.contains(val)) return false;
            }

            return true;
        }
    }
}
