package atrox.resources;

import atrox.dao.AccountDAO;
import atrox.dao.SessionDAO;
import atrox.model.Account;
import atrox.server.AtroxConfiguration;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.cobbzilla.wizard.resources.ResourceUtil.*;

@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path(AccountsResource.ENDPOINT)
@Service @Slf4j
public class AccountsResource extends AuthResourceBase<Account> {

    public static final String ENDPOINT = "/account";

    @Autowired private AtroxConfiguration configuration;
    @Autowired private SessionDAO sessionDAO;
    @Autowired @Getter private AccountDAO accountDAO;
    @Autowired @Getter private TemplatedMailService templatedMailService;

    @GET
    public Response me (@Context HttpContext ctx) {
        final Account found = userPrincipal(ctx);
        return found == null ? notFound() : ok(found);
    }

    @PUT
    public Response login (@Context HttpContext ctx, LoginRequest request) {
        if (request.hasName()) {
            try {
                Account account = accountDAO.authenticate(request);
                return ok(startSession(account != null ? account : accountDAO.anonymousAccount()));
            } catch (AuthenticationException e) {
                return ok(startSession(accountDAO.anonymousAccount()));
            }
        }
        return ok(startSession(accountDAO.anonymousAccount()));
    }

    private String startSession(Account account) { return sessionDAO.create(account); }

    @Override protected String getResetPasswordUrl(String token) { return configuration.getResetPasswordUrl(token); }
}
