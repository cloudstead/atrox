package atrox.dao;

import atrox.ApiConstants;
import atrox.dao.internal.AuditLogDAO;
import atrox.model.Account;
import atrox.model.auth.RegistrationRequest;
import atrox.server.AtroxConfiguration;
import cloudos.dao.AccountBaseDAOBase;
import cloudos.model.auth.AuthenticationException;
import cloudos.model.auth.LoginRequest;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.SimpleEmailMessage;
import org.cobbzilla.mail.TemplatedMail;
import org.cobbzilla.mail.service.TemplatedMailService;
import org.cobbzilla.wizard.model.HashedPassword;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.mail.service.TemplatedMailService.T_WELCOME;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@Repository @Slf4j
public class AccountDAO extends AccountBaseDAOBase<Account> {

    @Autowired private AtroxConfiguration configuration;
    @Autowired private TemplatedMailService mailService;
    @Autowired private AuditLogDAO audit;

    public Account findByEmail(String email) {
        return findByUniqueField("canonicalEmail", Account.canonicalizeEmail(email));
    }

    @Override public Account findByName(String name) { return findByEmail(name); }

    @Override public Object preCreate(@Valid Account account) {
        if (empty(account.getName())) account.setName(account.getEmail());
        account.setFirstName(".");
        account.setLastName(".");
        account.setHashedPassword(new HashedPassword(randomAlphanumeric(30)));
        account.setMobilePhone(ApiConstants.PLACEHOLDER_MOBILE_PHONE);
        account.setMobilePhoneCountryCode(1);
        return super.preCreate(account);
    }

    @Override public Account authenticate(LoginRequest login) throws AuthenticationException {

        final String name = login.getName();
        final Account account = findByEmail(name);

        audit.log(login, "authenticate", "starting for '"+name+"'");
        if (empty(name) || account == null) {
            audit.log(login, "authenticate", "no account found: '"+ name + "', return null");
            return null;
        }

        if (account.getHashedPassword().isCorrectPassword(login.getPassword())) {
            audit.log(login, "authenticate", "successful password login for " + name);
            return account;
        }
        audit.log(login, "authenticate", "unsuccessful login for "+ name+", return null");
        return null;
    }

    public Account register(RegistrationRequest request) {
        final String name = request.getName();
        final String password = request.getPassword();

        audit.log(request, "register", "starting for '"+name+"'");

        if (empty(name) || empty(password)) {
            audit.log(request, "register", "no name ("+name+") or password, returning anonymous account");
            return anonymousAccount();
        }

        final Account account = findByEmail(name);
        if (account != null) {
            audit.log(request, "register", "name exists ("+name+"), returning error");
            throw invalidEx("err.email.notUnique");
        }

        audit.log(request, "register", "creating account for: '"+ name + "'");

        Account newAccount = (Account) new Account().setEmail(name);
        newAccount.initEmailVerificationCode();

        newAccount = create(newAccount);
        sendWelcomeEmail(newAccount);

        return newAccount;
    }

    public Account registerAnonymous(Account account, RegistrationRequest request) {
        final String name = request.getName();
        final String password = request.getPassword();

        audit.log(request, "registerAnonymous", "starting for '"+name+"'");
        if (empty(name) || empty(password)) {
            audit.log(request, "registerAnonymous", "no name ("+name+") or password, returning null");
            return null;
        }

        final Account exists = findByEmail(name);
        if (exists != null) {
            audit.log(request, "registerAnonymous", "name exists ("+name+"), returning error");
            throw invalidEx("err.email.notUnique");
        }

        final Account found = findByEmail(account.getEmail());
        if (found == null) {
            audit.log(request, "registerAnonymous", "anon account does not exist ("+account.getEmail()+"), returning error");
            throw invalidEx("err.email.notFound");
        }

        if (!found.isAnonymous()) {
            audit.log(request, "registerAnonymous", "name ("+name+") is not anon user ("+found.getEmail()+"), returning error");
            throw invalidEx("err.email.notAnonymous");
        }

        audit.log(request, "registerAnonymous", "anon user "+found.getEmail()+" now registered as "+name+"");
        found.setName(name);
        found.setEmail(name);
        found.setPassword(request.getPassword());
        found.setAnonymous(false);
        found.initEmailVerificationCode();

        final Account updated = update(found);
        sendWelcomeEmail(updated);

        return updated;
    }

    private void sendWelcomeEmail(Account account) {
        final SimpleEmailMessage welcomeSender = configuration.getEmailSenderNames().get(T_WELCOME);
        final String code = account.initEmailVerificationCode();
        final TemplatedMail mail = new TemplatedMail()
                .setToEmail(account.getEmail())
//                .setToName(account.getFullName())
                .setFromName(welcomeSender.getFromName())
                .setFromEmail(welcomeSender.getFromEmail())
                .setTemplateName(T_WELCOME)
                .setParameter(TemplatedMailService.PARAM_ACCOUNT, account)
                .setParameter("activationUrl", configuration.getPublicUriBase() + "/#/activate/" + code);
        try {
            mailService.getMailSender().deliverMessage(mail);
        } catch (Exception e) {
            log.error("sendWelcomeEmail: Error sending email: "+e, e);
        }
    }

    public Account anonymousAccount() {
        final Account account = new Account();
        account.setEmail(ApiConstants.anonymousEmail());
        account.setAnonymous(true);
        return create(account);
    }
}
