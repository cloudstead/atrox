package histori.auth;

import histori.dao.SessionDAO;
import histori.model.Account;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.filters.auth.AuthProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service @Slf4j
public class HistoriAuthProvider implements AuthProvider<Account> {

    @Autowired private SessionDAO sessionDAO;

    @Override public Account find(String uuid) { return sessionDAO.find(uuid); }

}
