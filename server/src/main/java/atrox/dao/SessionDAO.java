package atrox.dao;

import atrox.model.Account;
import atrox.server.AtroxConfiguration;
import org.cobbzilla.wizard.dao.AbstractSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SessionDAO extends AbstractSessionDAO<Account> {

    @Autowired private AtroxConfiguration configuration;

    @Override protected String getPassphrase() { return configuration.getSessionPassphrase(); }
}
