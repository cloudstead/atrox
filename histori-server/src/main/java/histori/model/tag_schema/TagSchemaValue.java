package histori.model.tag_schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Comparator;

import static histori.model.CanonicalEntity.canonicalize;
import static histori.model.tag_schema.TagSchemaField.LAST_ACCESSED;

@NoArgsConstructor @AllArgsConstructor
public class TagSchemaValue {

    public static final Comparator<TagSchemaValue> SORT_NAME = new Comparator<TagSchemaValue>() {
        @Override public int compare(TagSchemaValue v1, TagSchemaValue v2) {
            int diff = v1.getCanonicalField().compareTo(v2.getCanonicalField());
            return diff != 0 ? diff : v1.getValue().compareTo(v2.getValue());
        }
    };

    @Getter @Setter private String field;
    @JsonIgnore public String getCanonicalField () { return canonicalize(getField()); }

    @Getter @Setter private String value;
    @JsonIgnore public String getCanonicalValue () { return canonicalize(getValue()); }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TagSchemaValue value1 = (TagSchemaValue) o;

        if (!getCanonicalField().equals(value1.getCanonicalField())) return false;
        return getCanonicalValue().equals(value1.getCanonicalValue());

    }

    @Override public int hashCode() {
        int result = getCanonicalField().hashCode();
        result = 31 * result + getCanonicalValue().hashCode();
        return result;
    }

    @JsonIgnore public boolean isHashable() { return !getCanonicalField().equals(LAST_ACCESSED); }

}
