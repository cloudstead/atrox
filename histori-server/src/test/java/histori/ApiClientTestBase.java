package histori;

import histori.model.support.AccountAuthResponse;
import histori.model.auth.RegistrationRequest;
import histori.server.DbSeedListener;
import histori.server.HistoriConfiguration;
import histori.server.HistoriServer;
import com.fasterxml.jackson.databind.JavaType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailSender;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailService;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.StreamConfigurationSource;
import org.cobbzilla.wizard.util.RestResponse;
import org.cobbzilla.wizardtest.resources.ApiDocsResourceIT;

import java.util.List;

import static histori.ApiConstants.ACCOUNTS_ENDPOINT;
import static histori.ApiConstants.API_TOKEN;
import static histori.ApiConstants.EP_REGISTER;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.assertTrue;

@Slf4j
public class ApiClientTestBase extends ApiDocsResourceIT<HistoriConfiguration, HistoriServer> {


    protected String getTestConfig() { return "histori-config-test.yml"; }

    @Override public void onStart(RestServer<HistoriConfiguration> server) { new DbSeedListener().onStart(server); }

    @Override protected List<ConfigurationSource> getConfigurations() {
        return new SingletonList<ConfigurationSource>(new StreamConfigurationSource(getTestConfig()));
    }

    @Override protected String getTokenHeader() { return API_TOKEN; }

    public MockTemplatedMailService getTemplatedMailService() {
        return getBean(MockTemplatedMailService.class);
    }

    public MockTemplatedMailSender getTemplatedMailSender() {
        return (MockTemplatedMailSender) getTemplatedMailService().getMailSender();
    }

    protected boolean skipAdminCreation() {
        return false;
    }

    public static final String REGISTER_URL = ACCOUNTS_ENDPOINT + EP_REGISTER;

    public AccountAuthResponse register(RegistrationRequest request) throws Exception {
        AccountAuthResponse response = fromJson(post(REGISTER_URL, toJson(request)).json, AccountAuthResponse.class);
        if (response != null) pushToken(response.getSessionId());
        return response;
    }

    public AccountAuthResponse newAnonymousAccount() throws Exception {
        final RegistrationRequest request = new RegistrationRequest();
        apiDocs.addNote("Register an anonymous account");
        AccountAuthResponse response = register(request);
        assertTrue(response.hasSessionId());
        assertTrue(response.getAccount().isAnonymous());
        return response;
    }

    public <T> SearchResults<T> simpleSearch(String url, JavaType resultType) throws Exception {
        apiDocs.addNote("search " + url + " (default search)");
        final RestResponse response = doGet(url);
        return JsonUtil.PUBLIC_MAPPER.readValue(response.json, resultType);
    }

    public <T> SearchResults<T> search(String url, ResultPage page, JavaType resultType) throws Exception {
        apiDocs.addNote("search " + url + " with query: " + page);
        final RestResponse response = doPost(url, toJson(page));
        return JsonUtil.PUBLIC_MAPPER.readValue(response.json, resultType);
    }

}
