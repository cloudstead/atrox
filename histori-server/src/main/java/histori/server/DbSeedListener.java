package histori.server;

import histori.dao.CanonicalEntityDAO;
import histori.model.CanonicalEntity;
import histori.model.Tag;
import histori.model.TagType;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerLifecycleListenerBase;

import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.reflect.ReflectionUtil.arrayClass;

public class DbSeedListener extends RestServerLifecycleListenerBase<HistoriConfiguration> {

    private static final Class<? extends CanonicalEntity>[] SEED_CLASSES = new Class[]{ TagType.class, Tag.class };

    @Override public void onStart(RestServer server) {

        final HistoriConfiguration configuration = (HistoriConfiguration) server.getConfiguration();

        for (Class<? extends CanonicalEntity> seedClass : SEED_CLASSES) populate(configuration, seedClass);

        super.onStart(server);
    }

    public void populate(HistoriConfiguration configuration, Class<? extends CanonicalEntity> type) {
        final CanonicalEntityDAO dao = (CanonicalEntityDAO) configuration.getDaoForEntityClass(type);
        final CanonicalEntity[] things = (CanonicalEntity[]) fromJsonOrDie(loadResourceAsStringOrDie("seed/"+type.getSimpleName()+".json"), arrayClass(type));
        for (CanonicalEntity thing : things) {
            if (dao.findByCanonicalName(thing.getCanonicalName()) == null) dao.create(thing);
        }
    }

}
