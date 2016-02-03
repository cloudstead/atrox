package atrox.server;

import lombok.Getter;
import lombok.Setter;

import org.cobbzilla.mail.sender.SmtpMailConfig;
import org.cobbzilla.mail.service.TemplatedMailSenderConfiguration;
import org.cobbzilla.wizard.cache.redis.HasRedisConfiguration;
import org.cobbzilla.wizard.cache.redis.RedisConfiguration;
import org.cobbzilla.wizard.server.config.HasDatabaseConfiguration;
import org.cobbzilla.wizard.server.config.RestServerConfiguration;

public class AtroxConfiguration extends RestServerConfiguration
    implements HasDatabaseConfiguration, TemplatedMailSenderConfiguration, HasRedisConfiguration {

    @Getter @Setter private RedisConfiguration redis;

    @Getter @Setter private SmtpMailConfig smtp;
    @Getter @Setter private String emailTemplateRoot;

}