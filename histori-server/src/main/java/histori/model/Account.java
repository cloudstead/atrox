package histori.model;

import cloudos.model.AccountBase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cobbzilla.wizard.filters.auth.TokenPrincipal;
import org.cobbzilla.wizard.model.shard.Shardable;

import javax.persistence.*;

@Entity @Accessors(chain=true)
@AttributeOverrides({@AttributeOverride(name = "name", column = @Column(updatable=true))})
public class Account extends AccountBase implements TokenPrincipal, Shardable {

    @Override public void beforeCreate() { if (!hasUuid()) initUuid(); }

    @Override public String getHashToShardField() { return "uuid"; }

    @Getter @Setter private boolean anonymous = false;
    @Getter @Setter private boolean subscriber = false;

    // Set by HistoriAuthFilter
    @JsonIgnore @Transient
    @Getter private String apiToken;
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }

}
