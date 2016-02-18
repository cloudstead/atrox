package histori.dao;

import histori.model.CanonicalEntity;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;

import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@Repository
public class CanonicalEntityDAO<E extends CanonicalEntity> extends VersionedEntityDAO<E> {

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
        if (empty(uuid)) return null;
        final E found = super.findByUuid(uuid);
        return found != null ? found : findByCanonicalName(uuid);
    }

    public E findByCanonicalName(String canonicalName) {
        if (empty(canonicalName)) return null;
        return findByUniqueField("canonicalName", canonicalize(canonicalName));
    }

    public E findOrCreateByCanonicalName(String canonicalName) {
        if (empty(canonicalName)) return null;
        final E found = findByCanonicalName(canonicalName);
        return found != null ? found : create(newEntity(canonicalName));
    }

    public E findOrCreateByCanonical(@Valid E entity) {
        final E found = findByCanonicalName(entity.getCanonicalName());
        return found != null ? found : create(entity);
    }
}
