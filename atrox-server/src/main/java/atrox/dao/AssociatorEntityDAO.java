package atrox.dao;

import atrox.model.Account;
import atrox.model.AccountOwnedEntity;
import org.cobbzilla.wizard.model.Identifiable;
import org.hibernate.criterion.Restrictions;

import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.util.reflect.ReflectionUtil.set;
import static org.cobbzilla.util.string.StringUtil.classAsFieldName;

public class AssociatorEntityDAO<E extends AccountOwnedEntity,
                                 T1 extends Identifiable,
                                 T2 extends Identifiable>
        extends AccountOwnedEntityDAO<E> {

    public E findByAssociation(Account account, T1 foo, T2 bar) {
        return uniqueResult(criteria().add(Restrictions.and(
                Restrictions.eq("owner", account.getUuid()),
                Restrictions.eq(classAsFieldName(foo), foo.getUuid()),
                Restrictions.eq(classAsFieldName(bar), bar.getUuid())
        )));
    }

    public E createOrUpdateByAssociation(Account account, E entity, T1 thing1, T2 thing2) {
        E found = findByAssociation(account, thing1, thing2);
        if (found == null) {
            found = instantiate(getEntityClass());
            set(found, classAsFieldName(thing1), thing1.getUuid());
            set(found, classAsFieldName(thing2), thing2.getUuid());
            found.setOwner(account.getUuid());
            found = create(found);
        }

        copy(found, entity);
        set(found, classAsFieldName(thing1), thing1.getUuid());
        set(found, classAsFieldName(thing2), thing2.getUuid());
        found.setOwner(account.getUuid());

        return found;
    }

}
