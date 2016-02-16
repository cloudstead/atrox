package histori.dao;

import histori.ApiConstants;
import histori.archive.EntityArchive;
import histori.dao.canonical.CanonicalEntityDAO;
import histori.dao.internal.EntityPointerDAO;
import histori.model.Account;
import histori.model.SocialEntity;
import histori.model.canonical.CanonicalEntity;
import histori.model.history.EntityHistory;
import histori.model.internal.EntityPointer;
import histori.model.support.EntityVisibility;
import histori.model.tag.GenericEntityTag;
import histori.server.HistoriConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.dao.HibernateCallbackImpl;
import org.cobbzilla.wizard.model.StrongIdentifiableBase;
import org.cobbzilla.wizard.spring.config.rdbms_archive.ArchiveHibernateTemplate;
import org.cobbzilla.wizard.validation.ValidationResult;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static histori.ApiConstants.BOUND_RANGE;
import static histori.ApiConstants.ENTITY_CLASS_MAP;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;
import static org.cobbzilla.util.reflect.ReflectionUtil.*;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@Slf4j
public abstract class SocialEntityDAO<E extends SocialEntity> extends AccountOwnedEntityDAO<E> {

    @Autowired protected ArchiveHibernateTemplate archiveHibernateTemplate;
    @Autowired private HistoriConfiguration configuration;

    public List<E> findByOwner (String uuid) { return findByField("owner", uuid); }

    @Override public Object preCreate(@Valid E entity) {

        Object ctx = super.preCreate(entity);
        populateAssociated(entity);
        incrementVersionAndArchive(entity);
        return ctx;
    }

    @Override public Object preUpdate(@Valid E entity) {

        Object ctx = super.preUpdate(entity);
        populateAssociated(entity);
        incrementVersionAndArchive(entity);
        return ctx;
    }

    @Override protected String formatBound(String entityAlias, String bound, String value) {
        switch (bound) {
            case BOUND_RANGE:
                final int sepPos = value.indexOf(ApiConstants.RANGE_SEP);
                if (sepPos == -1 || sepPos == value.length()-1) die("formatBound("+BOUND_RANGE+"): invalid value: "+value);
                final String start = value.substring(0, sepPos);
                final String end = value.substring(sepPos+1);
                return "("+entityAlias+".ctime >= "+start+" AND "+entityAlias+".ctime <= "+end+")";
        }
        return notSupported("Invalid bound: " + bound);
    }

    /**
     * Ensure proper version ordering and archiving. Called on preCreate and preUpdate.
     * Find the archived version with the highest version number
     *   - if the entity's version is less than or equal to this archived version, increment it
     *   - if the entity's version is greater than the archived version, keep it
     *   - if no archived version exists, keep it
     * @param entity The entity to version/archive
     */
    public void incrementVersionAndArchive(E entity) {

        final EntityArchive archive = newArchiveEntity();
        copy(archive, entity);

        // if earlier archives exist, find the one with the latest version
        final List<Object> args = new ArrayList<>();
        args.add(entity.getOwner());

        final String uniqueSql;
        if (entity instanceof EntityHistory) {
            EntityHistory history = (EntityHistory) entity;
            args.add(history.getCanonicalFieldValue());
            uniqueSql = "and t." + columnName(history.getCanonicalField()) + " = ? ";

        } else if (entity instanceof GenericEntityTag) {
            GenericEntityTag tag = (GenericEntityTag) entity;
            uniqueSql = "and t.entity = ? and t."+tag.tagField()+" = ? ";
            args.add(tag.getEntity());
            args.add(tag.tagFieldValue());

        } else {
            throw (Error) die("incrementVersionAndArchive: Unsupported type: "+entity.getClass().getName()+" ("+entity+")");
        }

        try {
            // any old versions here?
            final String sql = "select t.original_uuid, t.entity_version " +
                    "from " + getArchiveTableName() + " t " +
                    "where t.owner = ? " + uniqueSql + " " +
                    "and t.entity_version = (select max(t.entity_version) from "+getArchiveTableName()+" where t.owner = ? "+uniqueSql+")";
            final ResultSetBean results = configuration.execSql(sql, args.toArray());
            if (!results.isEmpty() && results.first().get(0) != null) {
                final String uuid = (String) results.first().get(0);
                final Integer latestArchivedVersion = (Integer) results.first().get(1);
                if (latestArchivedVersion > entity.getEntityVersion()) {
                    // entity version is too low, make it the next minor version
                    entity.setEntityVersion(latestArchivedVersion+1);
                    entity.setUuid(uuid);
                }
            }
            archive.setOriginalUuid(entity.getUuid());
            archive.setUuid(archive.getUuid()+"_"+Integer.toHexString(entity.getEntityVersion()));
            archiveHibernateTemplate.save(archive);

        } catch (SQLException e) {
            log.error("incrementVersionAndArchive: error saving archive: "+e, e);
        }
    }

    public String archiveClassName() { return getEntityClass().getName().replace(".model.", ".archive.")+"Archive"; }

    @Getter(lazy=true) private final String archiveTableName = initArchiveTableName();
    public String initArchiveTableName() {
        return ImprovedNamingStrategy.INSTANCE.classToTableName(getEntityClass().getSimpleName() + "Archive");
    }
    public String columnName (String propName) { return ImprovedNamingStrategy.INSTANCE.propertyToColumnName(propName); }

    @Getter(lazy=true) private final E entityProto = newEntity();
    public E newEntity() { return instantiate(getEntityClass()); }
    public EntityArchive newArchiveEntity() { return (EntityArchive) instantiate(archiveClassName()); }

    public DAO dao(String entityType) {
        final Class<?> entityClass = ENTITY_CLASS_MAP.get(entityType);
        return configuration.getDaoForEntityClass(entityClass);
    }

    public E populateAssociated(E newEntity, ValidationResult validationResult) {
        for (String associated : newEntity.getAssociated()) {
            final String uuidOrName = String.valueOf(ReflectionUtil.get(newEntity, associated));
            if (uuidOrName == null || uuidOrName.equals("null")) throw invalidEx("err."+associated+".empty");

            DAO associatedDao = dao(associated);
            SocialEntity associatedEntity;

            if (associatedDao instanceof EntityPointerDAO) {
                final Object dereferencedEntity = associatedDao.findByUuid(uuidOrName);
                if (dereferencedEntity == null) {
                    validationResult.addViolation("err."+associated+".notFound", associated+" was not found: "+uuidOrName, uuidOrName);
                    continue;
                } else {
                    associatedDao = dao(((EntityPointer) dereferencedEntity).getEntityType());
                }
            }

            associatedEntity = (SocialEntity) associatedDao.findByUuid(uuidOrName);
            if (associatedEntity == null) {
                if (associatedDao instanceof CanonicalEntityDAO) {
                    CanonicalEntity canonical = (CanonicalEntity) ((CanonicalEntityDAO) associatedDao).newEntity();
                    if (!StrongIdentifiableBase.isStrongUuid(uuidOrName)) {
                        // Create new canonical on the fly
                        canonical.setName(uuidOrName);
                        canonical = (CanonicalEntity) associatedDao.create(canonical);
                        ReflectionUtil.set(newEntity, associated, canonical.getUuid());
                        newEntity.addAssociation(canonical);
                    }
                } else {
                    validationResult.addViolation("err." + associated + ".notFound", associated + " was not found: " + uuidOrName, uuidOrName);
                }

            } else {
                set(newEntity, associated, associatedEntity.getUuid());
                newEntity.addAssociation(associatedEntity);
            }
        }
        if (!validationResult.isEmpty()) throw invalidEx(validationResult);
        return newEntity;
    }

    public E populateAssociated (E entity) { return populateAssociated(entity, null); }

    public static final String[] PARAMS_STARTSWITH = new String[]{"owner", "nameFragment"};

    public List findByFieldStartsWith(Account account, String field, String nameFragment) {

        final String queryString = "select x."+field+" " +
                "from "+getEntityClass().getSimpleName()+" x " +
                "where x."+field+" like :nameFragment " +
                "and ( x.owner = :owner or x.visibility = '"+EntityVisibility.everyone+"' ) " +
                "order by length(x.name) desc ";

        final Object[] values = {account.getUuid(), nameFragment+"%"};

        // todo: we can probably cache common stuff in memory for a while
        return (List) hibernateTemplate.execute(new HibernateCallbackImpl(queryString, PARAMS_STARTSWITH, values, 0, 10));
    }

}
