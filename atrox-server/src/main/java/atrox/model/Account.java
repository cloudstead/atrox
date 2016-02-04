package atrox.model;

import cloudos.model.AccountBase;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.cobbzilla.wizard.filters.auth.TokenPrincipal;

import javax.persistence.Entity;
import javax.persistence.Transient;

@Entity
public class Account extends AccountBase implements TokenPrincipal {

    // Set by AtroxAuthFilter
    @JsonIgnore @Transient
    @Getter private String apiToken;
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }

}
