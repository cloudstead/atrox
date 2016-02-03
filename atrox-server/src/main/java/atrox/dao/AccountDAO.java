package atrox.dao;

import atrox.model.Account;
import atrox.model.LoginAttempt;
import cloudos.dao.AccountBaseDAOBase;
import cloudos.model.auth.AuthenticationException;
import cloudos.model.auth.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository public class AccountDAO extends AccountBaseDAOBase<Account> {

    @Autowired private LoginAttemptDAO loginAttemptDAO;

    public Account findByEmail(String email) {
        return findByUniqueField("canonicalEmail", Account.canonicalizeEmail(email));
    }

    @Override public Account findByName(String name) { return findByEmail(name); }

    @Override public Account authenticate(LoginRequest login) throws AuthenticationException {

        final String name = login.getName();

        loginAttemptDAO.create(new LoginAttempt().setName(name).setNotes("authenticate starting"));
        if (!login.hasName()) {
            loginAttemptDAO.create(new LoginAttempt().setName("-empty-").setNotes("authenticate: anonymous login, return null"));
            return null;
        }

        final Account account = findByEmail(name);
        if (account == null) {
            loginAttemptDAO.create(new LoginAttempt().setName(name).setNotes("authenticate: creating new account for "+ name));
            Account created = create((Account) new Account().setName(name));
            loginAttemptDAO.create(new LoginAttempt().setName(name).setNotes("authenticate: successfully created new account for "+ name+" uuid="+created.getUuid()));
            return created;
        }

        if (login.hasName()) {
            if (login.hasPassword()) {
                if (account.isEmailVerified()
                        && account.getHashedPassword().isCorrectPassword(login.getPassword())) {
                    loginAttemptDAO.create(new LoginAttempt().setName(name).setNotes("authenticate: successful password login for " + name));
                    return account;
                }
            } else if (account.isEmailVerified()) {
                loginAttemptDAO.create(new LoginAttempt().setName(name).setNotes("authenticate: password-less login not allowed for verified email " + name+", return null"));
                return null;

            } else {
                // name but no password, email not verified. create a named anonymous account, they can save it later
                loginAttemptDAO.create(new LoginAttempt().setName(name).setNotes("authenticate: password-less login allowed for unverified email " + name+", return null"));
                return account;
            }
        }
        loginAttemptDAO.create(new LoginAttempt().setName(name).setNotes("authenticate: unsuccessful login for "+ name+", return null"));
        return null;
    }

    public Account anonymousAccount() {
        final Account account = new Account();
        account.initUuid();
        return account;
    }
}
