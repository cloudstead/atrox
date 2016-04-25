package histori.model.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import histori.model.Account;
import histori.model.AccountOwnedEntity;
import org.cobbzilla.wizard.dao.sql.SQLFieldTransformer;

public enum EntityVisibility implements SQLFieldTransformer {

    everyone, owner, hidden, deleted;

    @JsonCreator public static EntityVisibility create(String val) { return EntityVisibility.valueOf(val.toLowerCase()); }

    @JsonIgnore public boolean isEveryone() { return this == everyone; }

    public boolean isVisibleTo(AccountOwnedEntity entity, Account account) {
        if (account != null && account.isAdmin()) return this != deleted; // admin sees everything except deleted stuff
        switch (this) {
            case everyone: return true;
            case hidden: case owner: return account != null && entity.getOwner().equals(account.getUuid());
            case deleted: default: return false;
        }
    }

    public static EntityVisibility create (String val, EntityVisibility defaultValue) {
        try {
            return create(val);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    // It doesn't matter which instance we use, we are just calling a static method
    // But it's nice to have a more meaningful name for the variable, when you are looking in a calling class
    public static final EntityVisibility TRANSFORMER = everyone;
    @Override public Object sqlToObject(Object entity, Object input) {
        return valueOf(input.toString());
    }

}
