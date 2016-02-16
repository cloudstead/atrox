package histori.model;

import cloudos.model.AccountBase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.filters.auth.TokenPrincipal;

import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
public class Account extends AccountBase implements TokenPrincipal {

    @Getter @Setter private boolean anonymous = false;

    // Set by HistoriAuthFilter
    @JsonIgnore @Transient
    @Getter private String apiToken;
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }

}
