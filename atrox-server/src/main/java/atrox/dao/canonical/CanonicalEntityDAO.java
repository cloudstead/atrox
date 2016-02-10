package atrox.dao.canonical;

import atrox.dao.TaggableDAO;
import atrox.model.canonical.CanonicalEntity;
import atrox.model.history.EntityHistory;
import atrox.model.tag.GenericEntityTag;
import lombok.Getter;
import org.cobbzilla.wizard.dao.HibernateCallbackImpl;

import javax.validation.Valid;
import java.util.List;

import static org.cobbzilla.util.reflect.ReflectionUtil.getFirstTypeParam;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

public abstract class CanonicalEntityDAO<E extends CanonicalEntity> extends TaggableDAO<E> {

    @Getter(lazy=true) private final EntityHistory historyProto = initHistoryProto();
    private EntityHistory initHistoryProto() { return (EntityHistory) instantiate(getFirstTypeParam(getClass(), EntityHistory.class)); }

    @Override public Object preCreate(@Valid E entity) {
        if (findByCanonicalName(entity.getCanonicalName()) != null) throw invalidEx("err.name.notUnique");
        return super.preCreate(entity);
    }

    @Override public Object preUpdate(@Valid E entity) {
        // sanity check
        if (findByCanonicalName(entity.getCanonicalName()) == null) throw invalidEx("err.name.notFound");
        return super.preUpdate(entity);
    }

    public E newEntity (String name) { return (E) instantiate(getEntityClass()).setName(name); }

    @Override public E findByUuid(String uuid) {
        final E found = super.findByUuid(uuid);
        return found != null ? found : findByCanonicalName(uuid);
    }

    public E findByCanonicalName(String canonicalName) {
        return findByUniqueField("canonicalName", CanonicalEntity.canonicalize(canonicalName));
    }

    public static final String[] PARAMS_STARTSWITH = new String[]{"nameFragment"};

    public List<String> findByCanonicalNameStartsWith(String nameFragment) {

        final String queryString = "select x.name " +
                "from "+getEntityClass().getSimpleName()+" x " +
                "where x.canonicalName like :nameFragment " +
                "order by length(x.name) desc ";

        final Object[] values = {nameFragment+"%"};

        // todo: we can probably cache common stuff in memory for a while
        return (List) hibernateTemplate.execute(new HibernateCallbackImpl(queryString, PARAMS_STARTSWITH, values, 0, 10));
    }

    public E findByHistory(EntityHistory history) { return findByUuid(history.getCanonicalFieldValue()); }

    public E findByTag(GenericEntityTag tag) { return findByUuid(tag.getEntity()); }

}
