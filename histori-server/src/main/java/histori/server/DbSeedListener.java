package histori.server;

import histori.dao.CanonicalEntityDAO;
import histori.dao.PermalinkDAO;
import histori.model.CanonicalEntity;
import histori.model.Permalink;
import histori.model.Tag;
import histori.model.TagType;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerLifecycleListenerBase;

import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.reflect.ReflectionUtil.arrayClass;

@Slf4j
public class DbSeedListener extends RestServerLifecycleListenerBase<HistoriConfiguration> {

    private static final Class<? extends CanonicalEntity>[] SEED_CLASSES = new Class[]{
            TagType.class,
            Tag.class,
            Permalink.class
    };

    @Override public void onStart(RestServer server) {

        final HistoriConfiguration configuration = (HistoriConfiguration) server.getConfiguration();

        for (Class<? extends CanonicalEntity> seedClass : SEED_CLASSES) populate(configuration, seedClass);

        super.onStart(server);
    }

    public void populate(HistoriConfiguration configuration, Class<? extends CanonicalEntity> type) {
        final DAO dao = configuration.getDaoForEntityClass(type);
        final Identifiable[] things = (Identifiable[]) fromJsonOrDie(loadResourceAsStringOrDie("seed/" + type.getSimpleName() + ".json"), arrayClass(type));
        if (dao instanceof CanonicalEntityDAO) {
            final CanonicalEntityDAO canonicalDAO = (CanonicalEntityDAO) dao;
            for (Identifiable thing : things) {
                final CanonicalEntity canonical = (CanonicalEntity) thing;
                if (canonicalDAO.findByCanonicalName(canonical.getCanonicalName()) == null) canonicalDAO.create(canonical);
            }
        } else if (dao instanceof PermalinkDAO) {
            final PermalinkDAO permalinkDAO = (PermalinkDAO) dao;
            for (Identifiable thing : things) {
                final Permalink permalink = (Permalink) thing;
                if (permalinkDAO.findByName(permalink.getName()) == null) permalinkDAO.create(permalink);
            }
        }
    }

}
