package atrox.dao;

import atrox.model.Account;
import atrox.model.CanonicallyNamedEntity;
import org.cobbzilla.wizard.dao.HibernateCallbackImpl;

import java.util.List;

import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

public abstract class CanonicallyNamedEntityDAO<E extends CanonicallyNamedEntity> extends AccountOwnedEntityDAO<E> {

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

    public static final String[] PARAMS_NAMEFRAGMENT = new String[]{":nameFragment"};

    public List<String> findByCanonicalNameStartsWith(Account account, String nameFragment) {

        // todo: ensure results are either public or owned by account
        // todo later: also add results that have visibility=shared and where the caller is a follower of the owner)
        final String queryString = "select x.name " +
                "from "+getEntityClass().getSimpleName()+" x " +
                "where x.canonicalName like :nameFragment " +
                "order by length(x.name) desc ";

        final Object[] values = {nameFragment+"%"};

        return (List) hibernateTemplate.execute(new HibernateCallbackImpl(queryString, PARAMS_NAMEFRAGMENT, values, 0, 10));
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
