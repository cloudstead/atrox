package histori.model.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import histori.model.Account;
import histori.model.AccountOwnedEntity;

public enum EntityVisibility {

    everyone, owner, hidden, deleted;

    @JsonCreator public static EntityVisibility create(String val) { return EntityVisibility.valueOf(val.toLowerCase()); }

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

}
