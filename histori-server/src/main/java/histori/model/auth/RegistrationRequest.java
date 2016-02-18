package histori.model.auth;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class RegistrationRequest extends HistoriLoginRequest {

    public static final RegistrationRequest ANONYMOUS = new RegistrationRequest();

    public RegistrationRequest(String name, String password) { super(name, password); }

}
