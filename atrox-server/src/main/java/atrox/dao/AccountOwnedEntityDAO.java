package atrox.dao;

import atrox.ApiConstants;
import atrox.archive.EntityArchive;
import atrox.dao.internal.EntityPointerDAO;
import atrox.dao.tags.TagDAO;
import atrox.model.Account;
import atrox.model.AccountOwnedEntity;
import atrox.model.CanonicallyNamedEntity;
import atrox.model.internal.EntityPointer;
import atrox.model.support.EntityVisibility;
import atrox.model.support.TagOrder;
import atrox.model.support.TagSearchType;
import atrox.model.tags.EntityTag;
import atrox.server.AtroxConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
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

import static atrox.ApiConstants.*;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;
import static org.cobbzilla.util.reflect.ReflectionUtil.*;
import static org.cobbzilla.wizard.resources.ResourceUtil.invalidEx;

@Slf4j
public abstract class AccountOwnedEntityDAO<E extends AccountOwnedEntity> extends AbstractCRUDDAO<E> {

    @Autowired protected ArchiveHibernateTemplate archiveHibernateTemplate;
    @Autowired private AtroxConfiguration configuration;
    @Autowired private EntityPointerDAO entityPointerDAO;

    public List<E> findByOwner (String uuid) { return findByField("owner", uuid); }

    @Override public Object preCreate(@Valid E entity) {

        Object ctx = super.preCreate(entity);

        final ValidationResult validationResult = populateAssociated(entity.getOwner(), entity);
        if (!validationResult.isEmpty()) throw invalidEx(validationResult); // sanity check

        incrementVersionAndArchive(entity);
        entityPointerDAO.create(new EntityPointer(entity.getUuid(), entity.getClass().getSimpleName()));

        return ctx;
    }

    @Override public Object preUpdate(@Valid E entity) {

        Object ctx = super.preUpdate(entity);

        final ValidationResult validationResult = populateAssociated(entity.getOwner(), entity);
        if (!validationResult.isEmpty()) throw invalidEx(validationResult); // sanity check

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
                return "(x.ctime >= "+start+" AND x.ctime <= "+end+")";
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
        String[] uniqueProps = entity.getUniqueProperties();
        final List<Object> args = new ArrayList<>();
        args.add(entity.getOwner());

        String uniqueSql = "";
        for (String propName : uniqueProps) {
            args.add(ReflectionUtil.get(entity, propName));
            uniqueSql += "and t." + columnName(propName) + " = ? ";
        }
        try {
            // any old versions here?
            final String sql = "select max(t.entity_version) " +
                    "from " + getArchiveTableName() + " t " +
                    "where t.owner = ? " + uniqueSql;
            final ResultSetBean results = configuration.execSql(sql, args.toArray());
            if (!results.isEmpty() && results.first().get(0) != null) {
                final Integer latestArchivedVersion = (Integer) results.first().get(0);
                if (latestArchivedVersion > entity.getEntityVersion()) {
                    // entity version is too low, make it the next minor version
                    entity.setEntityVersion(latestArchivedVersion+1);
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

    public EntityArchive newArchiveEntity() { return (EntityArchive) instantiate(archiveClassName()); }

    public DAO dao(String entityType) {
        final Class<?> entityClass = ENTITY_CLASS_MAP.get(entityType);
        return configuration.getDaoForEntityClass(entityClass);
    }

    public ValidationResult populateAssociated(String account, E newEntity, ValidationResult validationResult) {
        for (String associated : newEntity.getAssociated()) {
            final String uuidOrName = String.valueOf(ReflectionUtil.get(newEntity, associated));
            if (uuidOrName == null || uuidOrName.equals("null")) throw invalidEx("err."+associated+".empty");

            DAO associatedDao = dao(associated);
            AccountOwnedEntity associatedEntity;

            if (associatedDao instanceof EntityPointerDAO) {
                final Object dereferencedEntity = associatedDao.findByUuid(uuidOrName);
                if (dereferencedEntity == null) {
                    validationResult.addViolation("err."+associated+".notFound", associated+" was not found: "+uuidOrName, uuidOrName);
                    continue;
                } else {
                    associatedDao = dao(((EntityPointer) dereferencedEntity).getEntityType());
                }
            }

            associatedEntity = (AccountOwnedEntity) associatedDao.findByUuid(uuidOrName);
            if (associatedEntity == null) {
                if (associatedDao instanceof CanonicallyNamedEntityDAO) {
                    CanonicallyNamedEntity canonical = (CanonicallyNamedEntity) ((CanonicallyNamedEntityDAO) associatedDao).newEntity();
                    if (!StrongIdentifiableBase.isStrongUuid(uuidOrName)) {
                        // Create new canonical on the fly
                        canonical.setName(uuidOrName);
                        canonical.setOwner(account);
                        canonical = (CanonicallyNamedEntity) associatedDao.create(canonical);
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
        return validationResult;
    }

    public ValidationResult populateAssociated (String account, E entity) { return populateAssociated(account, entity, new ValidationResult()); }

    public E populateTags (Account account, E entity, TagSearchType tagSearchType, TagOrder tagOrder) {
        for (String tagType: entity.getTagTypes()) {
            final TagDAO tagDao = (TagDAO) dao(tagType);
            final List<EntityTag> tags = tagDao.findTags(account, getEntityClass().getSimpleName(), entity.getUuid(), tagSearchType, tagOrder);
            entity.addTags(tags);
        }
        return entity;
    }

    public static final String[] PARAMS_STARTSWITH = new String[]{":owner", ":nameFragment"};

    public List findByFieldStartsWith(Account account, String field, String nameFragment) {

        final String queryString = "select x."+field+" " +
                "from "+getEntityClass().getSimpleName()+" x " +
                "where x."+field+" like :nameFragment " +
                "and ( x.owner = :owner or x.visibility = '"+EntityVisibility.everyone+"' ) " +
                "order by length(x.name) desc ";

        final Object[] values = {account.getUuid(), nameFragment+"%"};

        return (List) hibernateTemplate.execute(new HibernateCallbackImpl(queryString, PARAMS_STARTSWITH, values, 0, 10));
    }
}
