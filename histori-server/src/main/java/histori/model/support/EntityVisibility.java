package histori.model.support;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EntityVisibility {

    owner, everyone, hidden;

    @JsonCreator public static EntityVisibility create(String val) { return EntityVisibility.valueOf(val.toLowerCase()); }

    public static EntityVisibility create (String val, EntityVisibility defaultValue) {
        try {
            return create(val);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

}
