package histori.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@MappedSuperclass @Accessors(chain=true)
public class AccountOwnedEntity extends TaggableEntity {

    @Column(nullable = false, length = UUID_MAXLEN) @Getter
    @Setter private String owner;
    public boolean hasOwner() { return !empty(owner); }
    public boolean isOwner(String uuid) { return hasOwner() && getOwner().equals(uuid); }


}
