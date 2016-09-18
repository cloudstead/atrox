package histori.dao;

import histori.dao.shard.PreferredOwnerShardDAO;
import histori.model.Account;
import histori.model.PreferredOwner;
import lombok.Getter;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.json.JsonUtil.json;

@Repository
public class PreferredOwnerDAO extends ShardedEntityDAO<PreferredOwner, PreferredOwnerShardDAO> {

    @Autowired private DatabaseConfiguration database;
    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("preferred-owner"); }

    @Autowired private AccountDAO accountDAO;

    @Autowired private RedisService redisService;
    @Getter(lazy=true) private final RedisService cache = initCache();
    private RedisService initCache() { return redisService.prefixNamespace(PreferredOwnerDAO.class.getName(), null); }

    @Override public Object preCreate(@Valid PreferredOwner entity) {
        getCache().del(entity.getOwner());
        return super.preCreate(entity);
    }

    @Override public void delete(String uuid) {
        final PreferredOwner found = findByUuid(uuid);
        if (found != null) {
            getCache().del(found.getOwner());
            super.delete(uuid);
        }
    }

    public List<PreferredOwner> findByOwner(Account account) {
        final String accountUuid = account.getUuid();
        final List<PreferredOwner> found;
        final String cacheJson = getCache().get(accountUuid);
        if (cacheJson == null) {
            found = populateNames(account, findByField("owner", accountUuid));
            Collections.sort(found, PreferredOwner.SORT_PRIORITY);
            getCache().set(accountUuid, json(found), "EX", TimeUnit.DAYS.toSeconds(1));
        } else {
            found = Arrays.asList(json(cacheJson, PreferredOwner[].class));
        }
        return found;
    }

    private List<PreferredOwner> populateNames(Account account, List<PreferredOwner> owners) {
        boolean foundAccount = false;
        for (Iterator<PreferredOwner> iter = owners.iterator(); iter.hasNext(); ) {
            final PreferredOwner owner = iter.next();
            final Account found = accountDAO.findByUuid(owner.getPreferred());
            if (!foundAccount && found.getUuid().equals(account.getUuid())) foundAccount = true;
            if (found == null) {
                iter.remove();
            } else {
                owner.setName(found.getName());
            }
        }
        if (!foundAccount) owners.add(new PreferredOwner(account, account));
        return owners;
    }

    public PreferredOwner findByAccountAndUuid(Account account, String uuid) {
        return findByUniqueFields("owner", account.getUuid(), "uuid", uuid);
    }

    public PreferredOwner findByAccountAndPreferred(Account account, String preferred) {
        return findByUniqueFields("owner", account.getUuid(), "preferred", preferred);
    }

    public String findPreferredUuidsByOwner(Account account) {
        final StringBuilder b = new StringBuilder();
        for (PreferredOwner owner : findByOwner(account)) {
            if (b.length() > 0) b.append(",");
            b.append(owner.getPreferred());
        }
        return b.toString();
    }

}
