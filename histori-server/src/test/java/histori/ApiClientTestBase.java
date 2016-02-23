package histori;

import com.fasterxml.jackson.databind.JavaType;
import histori.model.Nexus;
import histori.model.NexusTag;
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
import org.cobbzilla.wizard.model.ResultPage;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.config.factory.ConfigurationSource;
import org.cobbzilla.wizard.server.config.factory.StreamConfigurationSource;
import org.cobbzilla.wizard.util.RestResponse;
import org.cobbzilla.wizardtest.resources.ApiDocsResourceIT;
import org.geojson.Point;

import java.util.List;
import java.util.Map;

import static histori.ApiConstants.*;
import static histori.model.CanonicalEntity.canonicalize;
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

    public <T> SearchResults<T> search(String url, ResultPage page, JavaType resultType) throws Exception {
        apiDocs.addNote("search " + url + " with query: " + page);
        final RestResponse response = doPost(url, toJson(page));
        return JsonUtil.PUBLIC_MAPPER.readValue(response.json, resultType);
    }

    public SearchResults<NexusSummary> search(String startDate, String endDate) throws Exception {
        return simpleSearch(SEARCH_ENDPOINT + EP_DATE + "/" + startDate + "/" + endDate, NexusSummary.SEARCH_RESULT_TYPE);
    }

    public Nexus dummyNexus () {
        final TimeRange range = randomTimeRange();
        return dummyNexus(range);
    }

    public Nexus dummyNexus (TimeRange range) {
        return newNexus(range.getStartPoint().toString(), range.getEndPoint().toString(), "nexus-"+randomName(), "headline-"+randomName());
    }

    public Nexus newNexus(String startDate, String endDate, String nexusName, String headline) {
        final Nexus nexus = new Nexus();
        nexus.setName(nexusName);
        nexus.setTimeRange(startDate, endDate);
        nexus.setGeo(new Point(0, 0));
        nexus.initCommentary().setHeadline(headline);
        return nexus;
    }

    public Nexus createNexus(Nexus nexus) throws Exception { return createNexus(nexus.getName(), nexus); }

    public Nexus createNexus(String nexusName, Nexus nexus) throws Exception {
        Nexus createdNexus = fromJson(put(NEXUS_ENDPOINT+"/"+urlEncode(nexusName), toJson(nexus)).json, Nexus.class);
        assertEquals(nexusName, createdNexus.getName());
        return createdNexus;
    }

    public void addTag(String nexusPath, NexusTag tag) throws Exception {
        final String canonical = canonicalize(tag.getTagName());
        final String tagPath = nexusPath + EP_TAGS + "/" + urlEncode(canonical);
        final RestResponse response = put(tagPath, toJson(tag));
        final NexusTag createdTag = fromJson(response.json, NexusTag.class);
        assertEquals(createdTag.getTagName(), canonical);
    }

}
