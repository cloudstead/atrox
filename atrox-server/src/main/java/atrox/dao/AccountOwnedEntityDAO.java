package atrox.dao;

import atrox.ApiConstants;
import atrox.dao.internal.EntityPointerDAO;
import atrox.model.AccountOwnedEntity;
import atrox.model.archive.EntityArchive;
import atrox.model.internal.EntityPointer;
import atrox.server.AtroxConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.jdbc.ResultSetBean;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.model.SemanticVersion;
import org.hibernate.cfg.ImprovedNamingStrategy;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static atrox.ApiConstants.BOUND_RANGE;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.notSupported;
import static org.cobbzilla.util.reflect.ReflectionUtil.copy;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@Slf4j
public abstract class AccountOwnedEntityDAO<E extends AccountOwnedEntity> extends AbstractCRUDDAO<E> {

    @Autowired private AtroxConfiguration configuration;
    @Autowired private EntityPointerDAO entityPointerDAO;

    public List<E> findByOwner (String uuid) { return findByField("owner", uuid); }

    @Override public Object preCreate(@Valid E entity) {

        Object ctx = super.preCreate(entity);
        incrementVersionAndArchive(entity);
        entityPointerDAO.create(new EntityPointer(entity.getUuid(), entity.getClass().getName()));

        return ctx;
    }

    @Override public Object preUpdate(@Valid E entity) {

        Object ctx = super.preUpdate(entity);
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

    public void incrementVersionAndArchive(E entity) {

        final EntityArchive archive = newArchiveEntity();
        copy(archive, entity);

        // if earlier archives exist, find the one with the latest version
        String[] uniqueProps = archive.getUniqueProperties();
        final List<String> args = new ArrayList<>();
        args.add(archive.getOwner());
        String uniqueSql = "";
        for (String propName : uniqueProps) {
            args.add((String) ReflectionUtil.get(archive, propName));
            uniqueSql = "and t." + columnName(propName) + " = ? ";
        }
        try {
            // any old versions here?
            final String sql = "select t.major_version, t.minor_version, t.patch_version " +
                    "from " + getArchiveTableName() + " t " +
                    "where t.owner = ? " + uniqueSql + " " +
                    "order by t.major_version DESC, t.minor_version DESC, t.patch_version DESC LIMIT 1";
            final ResultSetBean results = configuration.execSql(sql, args.toArray());
            if (!results.isEmpty()) {
                Map<String, Object> mostRecentArchive = results.first();
                final SemanticVersion nextVersion = new SemanticVersion(
                        mostRecentArchive.get("major_version").toString()+"."+
                        mostRecentArchive.get("minor_version").toString()+"."+
                        mostRecentArchive.get("patch_version").toString()
                ).incrementMinor();
                archive.setVersion(nextVersion);
                archive.setOriginalUuid(entity.getUuid());
                archive.setUuid(archive.getUuid()+"_"+nextVersion);
            }
            hibernateTemplate.save(archive);

        } catch (SQLException e) {
            log.error("preCreate: error saving archive: "+e, e);
        }
    }

    public String archiveClassName() { return getEntityClass().getName()+"Archive"; }

    @Getter(lazy=true) private final String archiveTableName = initArchiveTableName();
    public String initArchiveTableName() {
        return ImprovedNamingStrategy.INSTANCE.classToTableName(getEntityClass().getSimpleName() + "Archive");
    }
    public String columnName (String propName) { return ImprovedNamingStrategy.INSTANCE.propertyToColumnName(propName); }

    public EntityArchive newArchiveEntity() { return (EntityArchive) instantiate(archiveClassName()); }
}
