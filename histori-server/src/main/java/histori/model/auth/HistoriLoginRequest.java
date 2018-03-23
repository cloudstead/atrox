package histori.model.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;
import org.cobbzilla.wizard.auth.LoginRequest;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor
public class HistoriLoginRequest extends LoginRequest {

    public HistoriLoginRequest(String name, String password) {
        setName(name);
        setPassword(password);
    }

    @Override public boolean forceLowercase() { return false; }

    @JsonIgnore public boolean isEmpty () { return empty(getName()) && empty(getPassword()); }

}
