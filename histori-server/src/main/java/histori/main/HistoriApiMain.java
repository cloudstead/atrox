package histori.main;

import histori.model.auth.HistoriLoginRequest;
import histori.model.auth.RegistrationRequest;
import histori.model.support.AccountAuthResponse;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.main.MainApiBase;
import org.cobbzilla.wizard.main.MainApiOptionsBase;
import org.cobbzilla.wizard.util.RestResponse;

import static histori.ApiConstants.*;
import static histori.model.auth.RegistrationRequest.ANONYMOUS;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Slf4j
public abstract class HistoriApiMain<OPT extends MainApiOptionsBase> extends MainApiBase<OPT> {

    @Override protected Object buildLoginRequest(OPT options) {
        return new HistoriLoginRequest(options.getAccount(), options.getPassword());
    }

    @Override protected String getApiHeaderTokenName() { return API_TOKEN; }

    @Override protected String getLoginUri(String account) { return ACCOUNTS_ENDPOINT + EP_LOGIN; }

    @Override protected String getSessionId(RestResponse response) throws Exception {
        return fromJson(response.json, AccountAuthResponse.class).getSessionId();
    }

    @Override protected void setSecondFactor(Object loginRequest, String token) { /* todo */ }

    @Override protected void preRun() { login(); }

    @Override protected void login() {
        final OPT options = getOptions();
        final ApiClientBase api = getApiClient();
        final RestResponse response;
        final String registrationUri = ACCOUNTS_ENDPOINT + EP_REGISTER;
        if (!options.hasAccount()) {
            try {
                response = api.post(registrationUri, toJson(ANONYMOUS));
                api.pushToken(getSessionId(response));

            } catch (Exception e) {
                die("login: error registering new anonymous account: "+e);
            }
        } else {
            try {
                super.login();
            } catch (Exception e) {
                if (options.hasPassword()) {
                    out("login: error logging in as " + options.getAccount() + " (" + e + "), trying to register new account");
                    try {
                        response = api.post(registrationUri, toJson(new RegistrationRequest(options.getAccount(), options.getPassword())));
                        api.pushToken(getSessionId(response));

                    } catch (Exception e2) {
                        die("login: error registering as "+options.getAccount()+": "+e2);
                    }
                } else {
                    die("Error logging in as " + options.getAccount() + " (" + e + "), no password provided");
                }
            }
        }
    }

    @Override protected void handleAccountNotFound(String account) { ZillaRuntime.die("Account not found: "+account); }

}
