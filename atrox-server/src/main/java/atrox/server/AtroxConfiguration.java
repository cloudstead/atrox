package atrox.server;

import lombok.Getter;
import lombok.Setter;

import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.mail.sender.SmtpMailConfig;
import org.cobbzilla.mail.service.TemplatedMailSenderConfiguration;
import org.cobbzilla.wizard.cache.redis.HasRedisConfiguration;
import org.cobbzilla.wizard.cache.redis.RedisConfiguration;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration @Slf4j
public class AtroxConfiguration extends RestServerConfiguration
    implements HasDatabaseConfiguration, TemplatedMailSenderConfiguration, HasRedisConfiguration {

    @Setter private DatabaseConfiguration database;
    @Bean public DatabaseConfiguration getDatabase() { return database; }

    @Setter private RedisConfiguration redis;
    @Bean public RedisConfiguration getRedis() {
        if (redis == null) redis = new RedisConfiguration(getSessionPassphrase());
        return redis;
    }

    @Getter @Setter private SmtpMailConfig smtp;
    @Getter @Setter private String emailTemplateRoot;

    @Getter @Setter private String sessionPassphrase;

    public String getResetPasswordUrl(String token) {
        return new StringBuilder().append(getPublicUriBase()).append("/reset_password.html?key=").append(token).toString();
    }
}