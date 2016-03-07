package histori.dao;

import histori.model.Account;
import histori.model.Bookmark;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class BookmarkDAO extends AbstractCRUDDAO<Bookmark> {

    public List<Bookmark> findByOwner(Account account) { return findByField("owner", account.getUuid()); }

    public Bookmark findByAccountAndName(Account account, String name) {
        return findByUniqueFields("owner", account.getUuid(), "name", name);
    }

}
