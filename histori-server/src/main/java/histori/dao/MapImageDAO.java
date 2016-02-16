package histori.dao;

import histori.model.Account;
import histori.model.MapImage;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.hibernate.criterion.Restrictions.and;
import static org.hibernate.criterion.Restrictions.eq;

@Repository public class MapImageDAO extends AccountOwnedEntityDAO<MapImage> {

    public MapImage findByOwnerAndUri(String accountUuid, String storageUri) {
        return uniqueResult(criteria().add(and(
                eq("owner", accountUuid),
                eq("uri", storageUri))));
    }

    public MapImage findByOwnerAndName(String accountUuid, String name) {
        return uniqueResult(criteria().add(and(
                eq("owner", accountUuid),
                eq("name", name))));
    }

    public List<MapImage> findByOwner(Account account) { return findByField("owner", account.getUuid()); }
}
