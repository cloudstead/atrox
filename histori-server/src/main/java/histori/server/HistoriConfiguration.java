package histori.server;

import cloudos.server.asset.AssetStorageConfiguration;
import cloudos.service.asset.AssetStorageService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.DefaultedMap;
import org.apache.commons.lang3.StringUtils;
import org.cobbzilla.mail.SimpleEmailMessage;
import org.cobbzilla.mail.sender.SmtpMailConfig;
import org.cobbzilla.mail.service.TemplatedMailSenderConfiguration;
import org.cobbzilla.util.reflect.ReflectionUtil;
import org.cobbzilla.wizard.cache.redis.HasRedisConfiguration;
import org.cobbzilla.wizard.cache.redis.RedisConfiguration;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.server.config.RecaptchaConfig;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

@Configuration @Slf4j
public class HistoriConfiguration extends RestServerConfiguration
    implements HasDatabaseConfiguration, TemplatedMailSenderConfiguration, HasRedisConfiguration {

    public static final String RESET_PASSWORD_URI = "/?key=";

    @Setter private DatabaseConfiguration database;
    @Bean public DatabaseConfiguration getDatabase() { return database; }

    @Setter private RedisConfiguration redis;
    @Bean public RedisConfiguration getRedis() {
        if (redis == null) redis = new RedisConfiguration(getSessionPassphrase());
        return redis;
    }

    @Getter @Setter private SmtpMailConfig smtp;
    @Getter @Setter private Map<String, SimpleEmailMessage> emailSenderNames = new HashMap<>();
    @Getter @Setter private String emailTemplateRoot;

    @Getter @Setter private String sessionPassphrase;
    @Getter @Setter private Map<String, Integer> threadPoolSizes = new DefaultedMap(2);

    @Getter @Setter private RecaptchaConfig recaptcha;

    @Getter @Setter private AssetStorageConfiguration assetStorage;
    @Getter(lazy=true) private final AssetStorageService assetStorageService = initStorageService();
    public AssetStorageService initStorageService () { return AssetStorageService.build(assetStorage); }

    public String getResetPasswordUrl(String token) {
        return new StringBuilder().append(getPublicUriBase()).append(RESET_PASSWORD_URI).append(token).toString();
    }

    public String getTokenFromResetPasswordUrl (String url) {
        if (!url.startsWith(getPublicUriBase()+RESET_PASSWORD_URI)) die("getTokenFromResetPasswordUrl: invalid url: "+url);
        int lastEq = url.lastIndexOf('=');
        if (lastEq == -1 || lastEq == url.length()-1) die("getTokenFromResetPasswordUrl: invalid url: "+url);
        return url.substring(lastEq+1);
    }

    public static final Map<Class, DAO> daoCache = new ConcurrentHashMap<>();

    public DAO getDaoForEntityClass(Class entityClass) {
        DAO entityDao = daoCache.get(entityClass);
        if (entityDao == null) {
            entityDao = getBean(entityClass.getName().replace(".model.", ".dao.") + "DAO");
            daoCache.put(entityClass, entityDao);
        }
        return entityDao;
    }

    public DAO getDaoForArchiveClass(String type) {
        Class<? extends DAO> entityClass = ReflectionUtil.forName("histori.dao.archive."+ StringUtils.capitalize(type)+"ArchiveDAO");
        DAO entityDao = daoCache.get(entityClass);
        if (entityDao == null) {
            entityDao = getBean(entityClass);
            daoCache.put(entityClass, entityDao);
        }
        return entityDao;
    }
}