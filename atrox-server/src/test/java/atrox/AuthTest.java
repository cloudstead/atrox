package atrox;

import atrox.model.AccountAuthResponse;
import atrox.model.auth.RegistrationRequest;
import org.cobbzilla.mail.sender.mock.MockTemplatedMailSender;
import org.cobbzilla.wizardtest.RandomUtil;
import org.junit.Test;

import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.*;

public class AuthTest extends ApiClientTestBase {

    public static final String DOC_TARGET = "Authentication";
    public static final String REGISTER_URL = ApiConstants.ACCOUNTS_ENDPOINT + ApiConstants.EP_REGISTER;

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

    @Test public void testCreateAnonymousAccount () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "Register an anonymous account");
        newAnonymousAccount();
    }

    @Test public void testCreateAnonymousAccountAndThenRegister () throws Exception {

        apiDocs.startRecording(DOC_TARGET, "Register an anonymous account, then register it as a normal account");
        AccountAuthResponse anonResponse = newAnonymousAccount();

        String email = "X-"+RandomUtil.randomEmail();

        apiDocs.addNote("Call register again, and because we are logged in anonymously, this should register us as a normal user");
        AccountAuthResponse normalResponse = register((RegistrationRequest) new RegistrationRequest().setName(email).setPassword(email));

        assertNotEquals(anonResponse.getSessionId(), normalResponse.getSessionId()); // a new session
        assertEquals(anonResponse.getAccount().getUuid(), normalResponse.getAccount().getUuid()); // but the same account uuid
        assertFalse(normalResponse.getAccount().isAnonymous()); // and is no longer anonymous

        final MockTemplatedMailSender mockSender = getTemplatedMailSender();
        assertEquals(1, mockSender.messageCount());
        assertEquals(email, mockSender.getFirstMessage().getToEmail());
    }

}
