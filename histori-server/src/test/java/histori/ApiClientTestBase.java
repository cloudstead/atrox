package histori;

import com.fasterxml.jackson.databind.JavaType;
import histori.model.Nexus;
import histori.model.auth.RegistrationRequest;
import histori.model.support.AccountAuthResponse;
import histori.model.support.NexusSummary;
import histori.model.support.TimeRange;
import histori.server.DbSeedListener;
import histori.server.HistoriConfiguration;
import histori.server.HistoriServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailSender;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailService;
import org.cobbzilla.util.collection.SingletonList;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.StreamConfigurationSource;
import org.cobbzilla.wizard.util.RestResponse;
import org.cobbzilla.wizardtest.resources.ApiDocsResourceIT;
import org.cobbzilla.wizardtest.server.config.DummyRecaptchaConfig;
import org.geojson.Point;

import java.util.List;
import java.util.Map;

import static histori.ApiConstants.*;
import static histori.model.support.TimePoint.TP_SEP;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.string.StringUtil.urlEncode;
import static org.cobbzilla.wizardtest.RandomUtil.randomName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Slf4j
public class ApiClientTestBase extends ApiDocsResourceIT<HistoriConfiguration, HistoriServer> {

    public static final String ENV_PLACES_API_KEY = "GOOGLE_PLACES_API_KEY";
    public static final String ENV_EXPORT_FILE = ".histori-dev.env";

    protected String getTestConfig() { return "histori-config-test.yml"; }

    @Override protected Map<String, String> getServerEnvironment() throws Exception {
        final Map<String, String> env = CommandShell.loadShellExports(ENV_EXPORT_FILE);
        if (!env.containsKey(ENV_PLACES_API_KEY)) die("getServerEnvironment: no GOOGLE_PLACES_API_KEY found in ~/"+ENV_EXPORT_FILE);
        return env;
    }

    public boolean seedTestData() { return true; }

    @Override public void onStart(RestServer<HistoriConfiguration> server) {
        // disable captcha for tests
        server.getConfiguration().setRecaptcha(DummyRecaptchaConfig.instance);
        new DbSeedListener().seed(server, seedTestData());
    }

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

    protected boolean skipAdminCreation() { return false; }

    public static final String REGISTER_URL = ACCOUNTS_ENDPOINT + EP_REGISTER;

    public AccountAuthResponse register(RegistrationRequest request) throws Exception {
        AccountAuthResponse response = fromJson(post(REGISTER_URL, toJson(request)).json, AccountAuthResponse.class);
        if (response != null) pushToken(response.getSessionId());
        return response;
    }

    public AccountAuthResponse newAnonymousAccount() throws Exception {
        logout();
        final RegistrationRequest request = new RegistrationRequest();
        apiDocs.addNote("Register an anonymous account");
        AccountAuthResponse response = register(request);
        assertTrue(response.hasSessionId());
        assertTrue(response.getAccount().isAnonymous());
        return response;
    }

    public TimeRange randomTimeRange() {
        final int startYear = -1 * RandomUtils.nextInt(0, Integer.MAX_VALUE);
        final int startMonth = RandomUtils.nextInt(0, 12);
        final int startDay = RandomUtils.nextInt(0, 27);
        final String startDate = ""+startYear+ TP_SEP +startMonth + TP_SEP + startDay;
        final String endDate = ""+startYear+ TP_SEP +startMonth + TP_SEP + (startDay+1);
        return new TimeRange(startDate, endDate);
    }

    public <T> SearchResults<T> simpleSearch(String url, JavaType resultType) throws Exception {
        apiDocs.addNote("search " + url + " (default search)");
        final RestResponse response = doGet(url);
        return JsonUtil.PUBLIC_MAPPER.readValue(response.json, resultType);
    }

    public SearchResults<NexusSummary> search(String startDate, String endDate, String query) throws Exception {
        // search entire map area with caching disabled
        return simpleSearch(SEARCH_ENDPOINT + EP_QUERY + "/" + startDate + "/" + endDate + "/180/-180/180/-180?c=false&q="+urlEncode(query), NexusSummary.SEARCH_RESULT_TYPE);
    }

    public Nexus dummyNexus () {
        final TimeRange range = randomTimeRange();
        return dummyNexus(range);
    }

    public Nexus dummyNexus (TimeRange range) {
        return newNexus(range.getStartPoint().toString(), range.getEndPoint().toString(), "nexus-"+randomName(), "markdown-"+randomName());
    }

    public Nexus newNexus(String startDate, String endDate, String nexusName, String markdown) {
        final Nexus nexus = new Nexus();
        nexus.setName(nexusName);
        nexus.setTimeRange(startDate, endDate);
        nexus.setGeo(new Point(0, 0));
        nexus.setMarkdown(markdown);
        return nexus;
    }

    public Nexus createNexus(Nexus nexus) throws Exception { return createNexus(nexus.getName(), nexus); }

    public Nexus createNexus(String nexusName, Nexus nexus) throws Exception {
        Nexus createdNexus = fromJson(post(NEXUS_ENDPOINT+"/"+urlEncode(nexusName), toJson(nexus)).json, Nexus.class);
        assertEquals(nexusName, createdNexus.getName());
        return createdNexus;
    }
}
