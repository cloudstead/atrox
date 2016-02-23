package histori.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JavaType;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.StrongIdentifiableBase;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@MappedSuperclass @Accessors(chain=true)
public class AccountOwnedEntity extends StrongIdentifiableBase {

    @Transient @JsonIgnore public JavaType getSearchResultType() { return SearchResults.jsonType(getClass()); }

    @Column(length=UUID_MAXLEN, nullable=false)
    @Getter @Setter private String owner;
    public boolean hasOwner() { return !empty(owner); }
    public boolean isOwner(String uuid) { return hasOwner() && getOwner().equals(uuid); }

}
