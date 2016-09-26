package histori.model.tag_schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static histori.model.CanonicalEntity.canonicalize;

@NoArgsConstructor @AllArgsConstructor
public class TagSchemaValue {

    @Getter @Setter private String field;
    @Getter @Setter private String value;

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TagSchemaValue value1 = (TagSchemaValue) o;

        if (!canonicalize(getField()).equals(canonicalize(value1.getField()))) return false;
        return canonicalize(getValue()).equals(canonicalize(value1.getValue()));

    }

    @Override public int hashCode() {
        int result = canonicalize(getField()).hashCode();
        result = 31 * result + canonicalize(getValue()).hashCode();
        return result;
    }

}
