package histori.dao;

import histori.model.archive.EntityArchive;
import histori.model.VersionedEntity;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.dao.shard.AbstractShardedDAO;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@Slf4j
public class VersionedEntityDAO<E extends VersionedEntity> {

    /**
     * Ensure proper version ordering and archiving. Called on preCreate and preUpdate.
     * Find the archived version with the highest version number
     *   - if the entity's version is less than or equal to this archived version, increment it
     *   - if the entity's version is greater than the archived version, keep it
     *   - if no archived version exists, keep it
     * @param entity The entity to version/archive
     */
    public static <E extends VersionedEntity,
                   A extends EntityArchive,
                   ED extends AbstractShardedDAO<E, ? extends SingleShardDAO<E>>,
                   AD extends AbstractShardedDAO<A, ? extends SingleShardDAO<A>>>
    void incrementVersionAndArchive(E entity, ED dao, AD archiveDao) {

        final String[] identifiers = entity.getIdentifiers();
        if (identifiers == null) {
            log.info("incrementVersionAndArchive: entity had no identifiers, not archiving: "+entity);
        }

        // assign uuid if we don't have one yet
        if (empty(entity.getUuid())) entity.setUuid(uuid());

        final Class<E> entityClass = dao.getEntityClass();
        final A archive = newArchiveEntity(entityClass);
        copy(archive, entity);
        archive.setUuid(uuid());

        // if earlier archives exist, find the one with the latest version
        final List<Object> args = new ArrayList<>();
        args.addAll(Arrays.asList(identifiers));

        String uniqueSql = "1=1";
        for (String idField : entity.getIdentifierFields()) {
            uniqueSql += " and t." + idField + " = ? ";
        }

        Iterator<A> iterator = null;
        try {
            // any old versions here?
            final String hsql = "select t.identifier, t.version " +
                    "from " + archive.getClass().getSimpleName() + " t " +
                    "where " + uniqueSql + " " +
                    "order by t.version desc";
            iterator = archiveDao.iterate(toShardHash(entity), hsql, args);
            final A latestArchive = iterator.hasNext() ? iterator.next() : null;
            if (latestArchive != null && latestArchive.getIdentifier() != null) {
                final String uuid = latestArchive.getIdentifier();
                final int latestArchivedVersion = latestArchive.getVersion();
                if (latestArchivedVersion >= entity.getVersion()) {
                    // entity version is too low, make it the next logical version
                    entity.setVersion(latestArchivedVersion+1);
                    archive.setVersion(entity.getVersion());

                    // set UUID to what it was the first time it was created
                    entity.setUuid(uuid);
                }
            }

            archive.setIdentifier(archive.getIdentifier(entity));
            archiveDao.create(archive);

        } catch (Exception e) {
            die("incrementVersionAndArchive: error saving archive: "+e, e);
        } finally {
            archiveDao.closeIterator(iterator);
        }
    }

    protected static <E extends VersionedEntity> String toShardHash(E entity) {
        return String.valueOf(ReflectionUtil.get(entity, entity.getHashToShardField()));
    }

    public static <A extends EntityArchive> A newArchiveEntity(Class entityClass) { return (A) instantiate(archiveClassName(entityClass)); }

    public static String archiveClassName(Class entityClass) { return entityClass.getName().replace(".model.", ".model.archive.")+"Archive"; }

}
