package atrox.resources;

import atrox.ApiConstants;
import atrox.dao.AccountDAO;
import atrox.dao.SessionDAO;
import atrox.model.Account;
import atrox.model.AccountAuthResponse;
import atrox.server.AtroxConfiguration;
import cloudos.model.auth.AuthResponse;
import cloudos.model.auth.AuthenticationException;
import cloudos.model.auth.LoginRequest;
import cloudos.resources.AuthResourceBase;
import com.sun.jersey.api.core.HttpContext;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.service.TemplatedMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import static atrox.ApiConstants.ACCOUNTS_ENDPOINT;
import static cloudos.model.AccountBase.ERR_EMAIL_INVALID;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Path(ACCOUNTS_ENDPOINT)
@Service @Slf4j
public class AccountsResource extends AuthResourceBase<Account> {

    @Autowired private AtroxConfiguration configuration;
    @Autowired private SessionDAO sessionDAO;
    @Autowired @Getter private AccountDAO accountDAO;
    @Autowired @Getter private TemplatedMailService templatedMailService;

    @GET
    public Response me (@Context HttpContext ctx) {
        final Account found = userPrincipal(ctx);
        return found == null ? notFound() : ok(found);
    }

    @POST
    @Path("/auth/{email}")
    public Response loginOrRegister (@Context HttpContext ctx, @PathParam("email") String email, LoginRequest request) {

        final Account alreadyLoggedIn = optionalUserPrincipal(ctx);
        if (alreadyLoggedIn != null) throw invalidEx(ApiConstants.ERR_ALREADY_LOGGED_IN);

        if (!request.hasName()) return ok(startSession(accountDAO.anonymousAccount()));

        if (!email.equalsIgnoreCase(request.getName())) throw invalidEx(ERR_EMAIL_INVALID);

        try {
            request.setUserAgent(ctx.getRequest().getHeaderValue(HttpHeaders.USER_AGENT));
            Account account = accountDAO.authenticate(request);
            return ok(startSession(account != null ? account : accountDAO.anonymousAccount()));
        } catch (AuthenticationException e) {
            return ok(startSession(accountDAO.anonymousAccount()));
        }
    }

    private AuthResponse<Account> startSession(Account account) {
        return new AccountAuthResponse(sessionDAO.create(account), account);
    }

    @Override protected String getResetPasswordUrl(String token) { return configuration.getResetPasswordUrl(token); }
}
