package histori.model.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class RegistrationRequest {

    public RegistrationRequest (String email, String password) {
        this(email, email, password, null);
    }

    public RegistrationRequest (String name, String email, String password) {
        this(name, email, password, null);
    }

    @Getter @Setter public String name;
    public boolean hasName () { return !empty(name); }

    @Getter @Setter public String email;
    @Getter @Setter public String password;

    // optional - server-side resource can fill this in for other server-side code to use
    @JsonIgnore @Getter @Setter private String userAgent;

    @JsonIgnore public boolean isEmpty () { return empty(name) || empty(email) || empty(password); }

}
