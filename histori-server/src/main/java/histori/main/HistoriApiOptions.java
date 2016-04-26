package histori.main;

import org.cobbzilla.wizard.main.MainApiOptionsBase;

public class HistoriApiOptions extends MainApiOptionsBase {

    public static final String PASSWORD_ENV_VAR = "HISTORI_PASS";
    @Override protected String getPasswordEnvVarName() { return PASSWORD_ENV_VAR; }

    public static final String DEFAULT_API_BASE = "@HISTORI_API";
    @Override protected String getDefaultApiBaseUri() { return DEFAULT_API_BASE; }

    @Override protected boolean requireAccount() { return false; }
}
