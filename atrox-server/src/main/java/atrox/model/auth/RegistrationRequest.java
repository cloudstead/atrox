package atrox.model.auth;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class RegistrationRequest extends AtroxLoginRequest {

    public RegistrationRequest(String name, String password) { super(name, password); }

}
