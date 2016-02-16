package histori.dao;

import histori.model.Account;
import histori.server.HistoriConfiguration;
import org.cobbzilla.wizard.dao.AbstractSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class SessionDAO extends AbstractSessionDAO<Account> {

    @Autowired private HistoriConfiguration configuration;

    @Override protected String getPassphrase() { return configuration.getSessionPassphrase(); }
}
