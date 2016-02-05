package atrox.model.auth;

import cloudos.model.auth.LoginRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class AtroxLoginRequest extends LoginRequest {

    @Override public boolean forceLowercase() { return false; }

    @JsonIgnore public boolean isEmpty () { return empty(getName()) && empty(getPassword()); }

}
