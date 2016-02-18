package histori.model.support;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RelationshipType {

    part_of, lead_to, caused_by, influenced, allied_with, supported, opposed;

    @JsonCreator public static RelationshipType create (String val) { return valueOf(val.toLowerCase()); }

    public static boolean isValid (String val) {
        try {
            create(val);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

}
