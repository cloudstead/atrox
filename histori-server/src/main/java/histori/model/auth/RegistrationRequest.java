package histori.model.auth;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class RegistrationRequest extends HistoriLoginRequest {

    public RegistrationRequest(String name, String password) { super(name, password); }

}
