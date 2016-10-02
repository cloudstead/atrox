package histori.dao;

import histori.model.Account;
import histori.model.SpecialAuthorEntity;
import lombok.Getter;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.reflect.ReflectionUtil.arrayClass;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

public abstract class SpecialAuthorDAO<E extends SpecialAuthorEntity, D extends SingleShardDAO<E>> extends ShardedEntityDAO<E, D> {

    @Autowired protected DatabaseConfiguration database;
    @Autowired private AccountDAO accountDAO;

    @Autowired private RedisService redisService;
    @Getter(lazy=true) private final RedisService cache = initCache();
    private RedisService initCache() { return redisService.prefixNamespace(getClass().getName(), null); }

    protected boolean addSelf() { return false; }
    protected List<E> sort(List<E> list) { return list; }
    protected abstract String specialAuthorField();

    @Override public Object preCreate(@Valid E entity) {
        getCache().del(entity.getOwner());
        return super.preCreate(entity);
    }

    @Override public Object preUpdate(@Valid E entity) {
        getCache().del(entity.getOwner());
        return super.preUpdate(entity);
    }

    @Override public void delete(String uuid) {
        final E found = findByUuid(uuid);
        if (found != null) {
            getCache().del(found.getOwner());
            super.delete(uuid);
        }
    }

    public List<E> findByOwner(Account account) {
        final String accountUuid = account.getUuid();
        final List<E> found;
        final String cacheJson = getCache().get(accountUuid);
        if (cacheJson == null) {
            found = sort(populateNames(account, findByField("owner", accountUuid)));
            getCache().set(accountUuid, json(found), "EX", TimeUnit.DAYS.toSeconds(1));
        } else {
            final E[] cached = (E[]) json(cacheJson, arrayClass(getEntityClass()));
            found = Arrays.asList(cached);
        }
        return found;
    }

    private List<E> populateNames(Account account, List<E> owners) {
        boolean foundAccount = false;
        for (Iterator<E> iter = owners.iterator(); iter.hasNext(); ) {
            final E owner = iter.next();
            final Account found = accountDAO.findByUuid(owner.getSpecialAuthor());
            if (!foundAccount && found.getUuid().equals(account.getUuid())) foundAccount = true;
            if (found == null) {
                iter.remove();
            } else {
                owner.setName(found.getName());
            }
        }
        if (!foundAccount && addSelf()) {
            final E self = instantiate(getEntityClass());
            self.setOwner(account.getUuid());
            self.setName(account.getName());
            self.setSpecialAuthor(account.getUuid());
            self.setActive(true);
            owners.add(self);
        }
        return owners;
    }

    public List<E> findActiveByOwner(Account account) {
        final List<E> found = findByOwner(account);
        final List<E> active = new ArrayList<>();
        for (E owner : found) {
            if (owner.isActive()) active.add(owner);
        }
        return active;
    }

    public E findByAccountAndUuid(Account account, String uuid) {
        return findByUniqueFields("owner", account.getUuid(), "uuid", uuid);
    }

    public E findByAccountAndAuthor(Account account, String preferred) {
        return findByUniqueFields("owner", account.getUuid(), specialAuthorField(), preferred);
    }

    public String findActiveUuidsByOwner(Account account) {
        final StringBuilder b = new StringBuilder();
        for (String uuid : findActiveUuidListByOwner(account)) {
            if (b.length() > 0) b.append(",");
            b.append(uuid);
        }
        return b.toString();
    }

    public List<String> findActiveUuidListByOwner(Account account) {
        final List<String> uuids = new ArrayList<>();
        for (E owner : findActiveByOwner(account)) {
            uuids.add(owner.getSpecialAuthor());
        }
        return uuids;
    }

}
