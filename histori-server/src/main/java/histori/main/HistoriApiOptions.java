package histori.main;

import org.cobbzilla.wizard.main.MainApiOptionsBase;

public class HistoriApiOptions extends MainApiOptionsBase {

    public static final String PASSWORD_ENV_VAR = "HISTORI_PASS";
    @Override protected String getPasswordEnvVarName() { return PASSWORD_ENV_VAR; }

    @Override protected String getDefaultApiBaseUri() {
        return System.getenv().containsKey("HISTORI_API") ? System.getenv("HISTORI_API") : "http://127.0.0.1:9091/api";
    }

    @Override public boolean requireAccount() { return false; }
}
