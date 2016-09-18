package histori.dao;

import histori.dao.shard.BlockedOwnerShardDAO;
import histori.model.Account;
import histori.model.BlockedOwner;
import lombok.Getter;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.json.JsonUtil.json;

@Repository
public class BlockedOwnerDAO extends ShardedEntityDAO<BlockedOwner, BlockedOwnerShardDAO> {

    @Autowired private DatabaseConfiguration database;
    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("blocked-owner"); }

    @Autowired private AccountDAO accountDAO;

    @Autowired private RedisService redisService;
    @Getter(lazy=true) private final RedisService cache = initCache();
    private RedisService initCache() { return redisService.prefixNamespace(BlockedOwnerDAO.class.getName(), null); }

    @Override public Object preCreate(@Valid BlockedOwner entity) {
        getCache().del(entity.getOwner());
        return super.preCreate(entity);
    }

    @Override public void delete(String uuid) {
        final BlockedOwner found = findByUuid(uuid);
        if (found != null) getCache().del(found.getOwner());
        super.delete(uuid);
    }

    public List<BlockedOwner> findByOwner(Account account) {
        final String accountUuid = account.getUuid();
        final List<BlockedOwner> found;
        final String cacheJson = getCache().get(accountUuid);
        if (cacheJson == null) {
            found = populateNames(findByField("owner", accountUuid));
            getCache().set(accountUuid, json(found), "EX", TimeUnit.DAYS.toSeconds(1));
        } else {
            found = Arrays.asList(json(cacheJson, BlockedOwner[].class));
        }
        return found;
    }

    private List<BlockedOwner> populateNames(List<BlockedOwner> owners) {
        for (Iterator<BlockedOwner> iter = owners.iterator(); iter.hasNext(); ) {
            final BlockedOwner owner = iter.next();
            final Account account = accountDAO.findByUuid(owner.getBlocked());
            if (account == null) {
                iter.remove();
            } else {
                owner.setName(account.getName());
            }
        }
        return owners;
    }

    public BlockedOwner findByAccountAndUuid(Account account, String uuid) {
        return findByUniqueFields("owner", account.getUuid(), "uuid", uuid);
    }

    public BlockedOwner findByAccountAndBlocked(Account account, String blockUuid) {
        return findByUniqueFields("owner", account.getUuid(), "blocked", blockUuid);
    }

    public String findBlockedUuidsByOwner(Account account) {
        final StringBuilder b = new StringBuilder();
        for (BlockedOwner blocked : findByOwner(account)) {
            if (b.length() > 0) b.append(",");
            b.append(blocked.getBlocked());
        }
        return b.toString();
    }
}
