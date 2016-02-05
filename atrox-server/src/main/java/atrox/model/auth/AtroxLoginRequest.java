package atrox.model.auth;

import cloudos.model.auth.LoginRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor
public class AtroxLoginRequest extends LoginRequest {

    public AtroxLoginRequest (String name, String password) {
        setName(name);
        setPassword(password);
    }

    @Override public boolean forceLowercase() { return false; }

    @JsonIgnore public boolean isEmpty () { return empty(getName()) && empty(getPassword()); }

}
