package histori.dao;

import histori.model.Account;
import histori.model.MapImage;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository public class MapImageDAO extends AbstractCRUDDAO<MapImage> {

    public MapImage findByOwnerAndUri(String accountUuid, String storageUri) {
        return findByUniqueFields("owner", accountUuid, "uri", storageUri);
    }

    public MapImage findByOwnerAndName(String accountUuid, String name) {
        return findByUniqueFields("owner", accountUuid, "name", name);
    }

    public List<MapImage> findByOwner(Account account) { return findByField("owner", account.getUuid()); }
}
