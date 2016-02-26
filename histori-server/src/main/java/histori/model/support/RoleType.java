package histori.model.support;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum RoleType {

    participant, combatant, observer, commander, founder;

    @JsonCreator public static RoleType create (String val) { return valueOf(val.toLowerCase()); }

    public static boolean isValid (String val) {
        try {
            create(val);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

}
