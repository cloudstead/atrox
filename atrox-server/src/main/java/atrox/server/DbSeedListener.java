package atrox.server;

import atrox.dao.canonical.CanonicalEntityDAO;
import atrox.model.canonical.*;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerLifecycleListenerBase;

import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;

public class DbSeedListener extends RestServerLifecycleListenerBase<AtroxConfiguration> {

    private static final Class<? extends CanonicalEntity>[] SEED_CLASSES = new Class[]{
            Idea.class, WorldActor.class, WorldEvent.class, IncidentType.class, ImpactType.class
    };

    @Override public void onStart(RestServer server) {

        final AtroxConfiguration configuration = (AtroxConfiguration) server.getConfiguration();

        for (Class<? extends CanonicalEntity> seedClass : SEED_CLASSES) populate(configuration, seedClass);

        super.onStart(server);
    }

    public void populate(AtroxConfiguration configuration, Class<? extends CanonicalEntity> type) {
        final CanonicalEntityDAO dao = (CanonicalEntityDAO) configuration.getDaoForEntityClass(type);
        for (String name : fromJsonOrDie(loadResourceAsStringOrDie("seed/"+type.getSimpleName()+".js"), String[].class)) {
            if (dao.findByCanonicalName(name) == null) dao.create(dao.newEntity(name));
        }
    }

}
