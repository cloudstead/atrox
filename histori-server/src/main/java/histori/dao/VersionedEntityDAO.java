package histori.dao;

import histori.archive.EntityArchive;
import histori.model.VersionedEntity;
import histori.server.HistoriConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.spring.config.rdbms_archive.ArchiveHibernateTemplate;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@Slf4j
public class VersionedEntityDAO<E extends VersionedEntity> extends AbstractCRUDDAO<E> {

    @Autowired protected ArchiveHibernateTemplate archiveHibernateTemplate;
    @Autowired private HistoriConfiguration configuration;

    @Override public Object preCreate(@Valid E entity) {

        Object ctx = super.preCreate(entity);
        incrementVersionAndArchive(entity);
        return ctx;
    }

    @Override public Object preUpdate(@Valid E entity) {

        Object ctx = super.preUpdate(entity);
        incrementVersionAndArchive(entity);
        return ctx;
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

        final String[] identifiers = entity.getIdentifiers();
        if (identifiers == null) {
            log.info("incrementVersionAndArchive: entity had no identifiers, not archiving: "+entity);
        }

        // assign uuid if we don't have one yet
        if (empty(entity.getUuid())) entity.setUuid(uuid());

        final EntityArchive archive = newArchiveEntity();
        copy(archive, entity);
        if (archive.archiveUuid()) {
            archive.setUuid(uuid());
            archive.setOriginalUuid(entity.getUuid());
        }

        // if earlier archives exist, find the one with the latest version
        final List<Object> args = new ArrayList<>();
        args.addAll(Arrays.asList(identifiers));

        String uniqueSql = "1=1";
        for (String idField : entity.getIdentifierFields()) {
            uniqueSql += " and t." + columnName(idField) + " = ? ";
        }

        try {
            // any old versions here?
            final String sql = "select t.original_uuid, t.version " +
                    "from " + getArchiveTableName() + " t " +
                    "where " + uniqueSql + " " +
                    "order by t.version desc limit 1";
            final ResultSetBean results = configuration.execSql(sql, args.toArray());
            if (!results.isEmpty() && results.first().get("original_uuid") != null) {
                final String uuid = (String) results.first().get("original_uuid");
                final Integer latestArchivedVersion = (Integer) results.first().get("version");
                if (latestArchivedVersion >= entity.getVersion()) {
                    // entity version is too low, make it the next logical version
                    entity.setVersion(latestArchivedVersion+1);
                    archive.setVersion(entity.getVersion());

                    // set UUID to what it was the first time it was created
                    entity.setUuid(uuid);
                }
            }
            archiveHibernateTemplate.save(archive);

        } catch (SQLException e) {
            die("incrementVersionAndArchive: error saving archive: "+e, e);
        }
    }

    public EntityArchive newArchiveEntity() { return (EntityArchive) instantiate(archiveClassName()); }

    public String archiveClassName() { return getEntityClass().getName().replace(".model.", ".archive.")+"Archive"; }

    @Getter(lazy=true) private final String archiveTableName = initArchiveTableName();
    public String initArchiveTableName() {
        return ImprovedNamingStrategy.INSTANCE.classToTableName(getEntityClass().getSimpleName() + "Archive");
    }
    public String columnName (String propName) { return ImprovedNamingStrategy.INSTANCE.propertyToColumnName(propName); }
}
