package atrox.dao;

import atrox.model.Account;
import atrox.model.CanonicallyNamedEntity;
import org.cobbzilla.wizard.dao.HibernateCallbackImpl;

import java.util.List;

import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

public abstract class CanonicallyNamedEntityDAO<E extends CanonicallyNamedEntity> extends AccountOwnedEntityDAO<E> {

    @Override public E findByUuid(String uuid) {
        final E found = super.findByUuid(uuid);
        return found != null ? found : findByCanonicalName(uuid);
    }

    public E findByCanonicalName(E entity) {
        return findByCanonicalName(entity.getCanonicalName());
    }

    public E findByCanonicalName(String canonicalName) {
        return findByUniqueField("canonicalName", canonicalName);
    }

    public E findOrCreateByCanonicalName(E entity) {
        E found = findByCanonicalName(entity);
        return found != null ? found : create(entity);
    }

    public static final String[] PARAMS_STARTSWITH = new String[]{":nameFragment"};

    public List<String> findByCanonicalNameStartsWith(String nameFragment) {

        final String queryString = "select x.name " +
                "from "+getEntityClass().getSimpleName()+" x " +
                "where x.canonicalName like :nameFragment " +
                "order by length(x.name) desc ";

        final Object[] values = {nameFragment+"%"};

        return (List) hibernateTemplate.execute(new HibernateCallbackImpl(queryString, PARAMS_STARTSWITH, values, 0, 10));
    }

    public E findOrCreateByCanonicalName(Account account, String name) {
        E found = findByCanonicalName(name);
        if (found == null) {
            found = instantiate(getEntityClass());
            found.setName(name);
            found.setOwner(account.getUuid());
            found = create(found);
        }
        return found;
    }
}
