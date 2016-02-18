package histori.resources;

import histori.ApiConstants;
import histori.dao.AccountDAO;
import histori.dao.SessionDAO;
import histori.model.Account;
import histori.model.support.AccountAuthResponse;
import histori.model.auth.HistoriLoginRequest;
import histori.model.auth.RegistrationRequest;
import histori.server.HistoriConfiguration;
import cloudos.model.auth.AuthResponse;
import cloudos.model.auth.AuthenticationException;
import cloudos.resources.AuthResourceBase;
import com.sun.jersey.api.core.HttpContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.service.TemplatedMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static histori.ApiConstants.*;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(ACCOUNTS_ENDPOINT)
@Service @Slf4j
public class AccountsResource extends AuthResourceBase<Account> {

    @Autowired private HistoriConfiguration configuration;
    @Autowired private SessionDAO sessionDAO;
    @Autowired @Getter private AccountDAO accountDAO;
    @Autowired @Getter private TemplatedMailService templatedMailService;

    /**
     * Get info about the currently logged in user
     * @param ctx session info
     * @return The Account object for the current user, or 404 Not Found if no user is logged in
     */
    @GET
    public Response me (@Context HttpContext ctx) {
        Account found = optionalUserPrincipal(ctx);
        if (found == null) return notFound();
        found = accountDAO.findByUuid(found.getUuid());
        return (found == null) ? notFound() : ok(found);
    }

    /**
     * Login. Upon successful login, this returns an AccountAuthResponse containing the session ID and account information.
     * If there is already a logged in user, a 422 error is returned. If the username/password does not match an existing user,
     * then a 404 error is returned.
     * @param ctx session info
     * @return Upon success, an AccountAuthResponse containing the session ID and account information
     */
    @POST
    @Path(EP_LOGIN)
    public Response login (@Context HttpContext ctx, HistoriLoginRequest request) {

        final Account alreadyLoggedIn = optionalUserPrincipal(ctx);
        if (alreadyLoggedIn != null) throw invalidEx(ApiConstants.ERR_ALREADY_LOGGED_IN);

        try {
            request.setUserAgent(ctx.getRequest().getHeaderValue(HttpHeaders.USER_AGENT));
            Account account = accountDAO.authenticate(request);
            return account != null ? ok(startSession(account)) : notFound(request.getName());

        } catch (AuthenticationException e) {
            log.warn("login: unexpected error: "+e, e);
            return notFound(request.getName());
        }
    }

    /**
     * Register a new account. There are a few different scenarios for this endpoint:
     *  - If no user is logged in:
     *    - If the request has no login name (email) or password, an anonymous account is created
     *    - If the request includes a name+password, then a normal account is created
     *  - If the user has already logged in
     *    - If the currently logged-in account is non-anonymous, a 422 error is returned
     *    - If the currently logged-in account is anonymous
     *      - If this request includes a name+password, the anonymous account is updated to a normal account
     *      - If this request does not include a name+password, nothing happens and the same anonymous account is returned
     * @param ctx session info
     * @return Upon success, an AccountAuthResponse containing the session ID and account information
     */
    @POST
    @Path(EP_REGISTER)
    public Response register (@Context HttpContext ctx, @Valid RegistrationRequest request) {

        request.setUserAgent(ctx.getRequest().getHeaderValue(HttpHeaders.USER_AGENT));

        final Account alreadyLoggedIn = optionalUserPrincipal(ctx);
        final Account account;
        if (alreadyLoggedIn != null) {
            if (!alreadyLoggedIn.isAnonymous()) throw invalidEx(ApiConstants.ERR_ALREADY_LOGGED_IN);

            // already logged in, but this request has no name/password -- just start a new session
            if (request.isEmpty()) return ok(startSession(alreadyLoggedIn));

            // try to convert anonymous account into normal account
            return ok(startSession(accountDAO.registerAnonymous(alreadyLoggedIn, request)));
        }

        account = accountDAO.register(request);
        return ok(startSession(account != null ? account : accountDAO.anonymousAccount()));
    }

    private AuthResponse<Account> startSession(Account account) {
        return new AccountAuthResponse(sessionDAO.create(account), account);
    }

    @Override protected String getResetPasswordUrl(String token) { return configuration.getResetPasswordUrl(token); }
}
