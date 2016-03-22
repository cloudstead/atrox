package histori.server;

import histori.dao.AccountDAO;
import histori.dao.CanonicalEntityDAO;
import histori.dao.ShardedEntityDAO;
import histori.model.*;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.model.HashedPassword;
import org.cobbzilla.wizard.model.Identifiable;
import org.cobbzilla.wizard.model.shard.Shardable;
import org.cobbzilla.wizard.server.RestServer;
import org.cobbzilla.wizard.server.RestServerLifecycleListenerBase;

import java.util.Map;

import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.reflect.ReflectionUtil.arrayClass;
import static org.cobbzilla.util.reflect.ReflectionUtil.get;

@Slf4j
public class DbSeedListener extends RestServerLifecycleListenerBase<HistoriConfiguration> {

    private static final Class<? extends CanonicalEntity>[] TEST_SEED_CLASSES = new Class[]{
            TagType.class,
            Tag.class,
            Permalink.class
    };

    private static final Class<? extends CanonicalEntity>[] PROD_SEED_CLASSES = new Class[]{
            TagType.class,
            Permalink.class
    };

    @Override public void onStart(RestServer server) { seed(server, false); }

    public void seed(RestServer server, boolean includeTestData) {

        final HistoriConfiguration configuration = (HistoriConfiguration) server.getConfiguration();

        final Class<? extends CanonicalEntity>[] seedClasses = includeTestData ? TEST_SEED_CLASSES : PROD_SEED_CLASSES;

        for (Class<? extends CanonicalEntity> seedClass : seedClasses) populate(configuration, seedClass);

        if (includeTestData || Boolean.valueOf(System.getenv("HISTORI_CREATE_SUPERUSER"))) {
            final Map<String, String> env = server.getConfiguration().getEnvironment();
            if (env.containsKey("HISTORI_SUPERUSER") && env.containsKey("HISTORI_SUPERUSER_PASS")) {
                final AccountDAO accountDAO = configuration.getBean(AccountDAO.class);
                if (accountDAO.adminsExist()) {
                    log.info("Admin accounts already exist, not creating new superuser");
                } else {
                    String name = env.get("HISTORI_SUPERUSER");
                    final Account created = accountDAO.create((Account) new Account()
                            .setAnonymous(false)
                            .setHashedPassword(new HashedPassword(env.get("HISTORI_SUPERUSER_PASS")))
                            .setAccountName(name)
                            .setAdmin(true)
                            .setEmail(name)
                            .setName(name));
                    log.info("Created superuser account " + name + ": " + created.getUuid());
                }
            }
        }
        super.onStart(server);
    }

    public void populate(HistoriConfiguration configuration, Class<? extends CanonicalEntity> type) {
        final DAO dao = configuration.getDaoForEntityType(type);
        final Identifiable[] things = (Identifiable[]) fromJsonOrDie(loadResourceAsStringOrDie("seed/" + type.getSimpleName() + ".json"), arrayClass(type));
        if (dao instanceof CanonicalEntityDAO) {
            final CanonicalEntityDAO canonicalDAO = (CanonicalEntityDAO) dao;
            for (Identifiable thing : things) {
                final CanonicalEntity canonical = (CanonicalEntity) thing;
                if (canonicalDAO.findByCanonicalName(canonical.getCanonicalName()) == null) canonicalDAO.create(canonical);
            }
        } else if (dao instanceof ShardedEntityDAO) {
            final ShardedEntityDAO shardedEntityDAO = (ShardedEntityDAO) dao;
            for (Identifiable thing : things) {
                final Shardable entity = (Shardable) thing;
                if (shardedEntityDAO.findByName((String) get(entity, "name")) == null) {
                    shardedEntityDAO.create(entity);
                }
            }
        }
    }

}
