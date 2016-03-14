package histori.dao;

import histori.model.Account;
import histori.model.Nexus;
import histori.model.NexusTag;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.cobbzilla.wizard.dao.shard.SingleShardDAO;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.Valid;
import java.util.List;

import static histori.model.TagType.EVENT_TYPE;
import static org.hibernate.criterion.Restrictions.*;

public class NexusShardDAO extends VersionedEntityDAO<Nexus> implements SingleShardDAO<Nexus> {

    @Getter @Setter private DatabaseConfiguration database;

    @Autowired private SuperNexusDAO superNexusDAO;
    @Autowired private RedisService redisService;

    @Getter(lazy=true) private final RedisService nexusCache = initNexusCache();
    private RedisService initNexusCache() { return redisService.prefixNamespace("nexus-cache:", null); }

    @Override public Object preCreate(@Valid Nexus entity) {
        entity.prepareForSave();

        // ensure tag is present, or create it if not
        if (entity.hasNexusType()) {
            return new NexusTag().setTagName(entity.getNexusType()).setTagType(EVENT_TYPE);
        }

        return super.preCreate(entity);
    }

    @Override public Object preUpdate(@Valid Nexus entity) {
        entity.prepareForSave();

        // ensure event_type tag corresponding to nexusType is present, or create it if not
        if (entity.hasNexusType()) {
            // what tags already exist?
            final NexusTag typeTag = new NexusTag().setTagName(entity.getNexusType()).setTagType(EVENT_TYPE);
            if (!entity.hasExactTag(typeTag)) {
                entity.addTag(typeTag);
                return typeTag;
            } else {
                // nexusType already matches one of the event_type tags
            }
        } else {
            entity.setNexusType(entity.getFirstEventType());
        }

        return super.preUpdate(entity);
    }

    @Override public Nexus create(Nexus entity)         { return AbstractCRUDDAO.create(entity, this); }
    @Override public Nexus createOrUpdate(Nexus entity) { return AbstractCRUDDAO.createOrUpdate(entity, this); }
    @Override public Nexus update(Nexus entity)         { return AbstractCRUDDAO.update(entity, this);     }

    @Override public Nexus postCreate(Nexus entity, Object context) {
        getNexusCache().set(entity.getUuid(), context.toString());
        superNexusDAO.updateSuperNexus(entity);
        return super.postCreate(entity, context);
    }

    @Override public Nexus postUpdate(@Valid Nexus entity, Object context) {
        getNexusCache().set(entity.getUuid(), context.toString());
        superNexusDAO.updateSuperNexus(entity);
        return super.postUpdate(entity, context);
    }

    public List<Nexus> findByName(String name) { return findByField("name", name); }

    public Nexus findByOwnerAndName(Account account, String name) {
        return findByUniqueFields("owner", account.getUuid(), "name", name);
    }

    public Nexus findByOwnerAndNameOrUuid(Account account, String nameOrUuid) {
        return uniqueResult(criteria().add(and(
                eq("owner", account.getUuid()),
                or( eq("uuid", nameOrUuid),
                        eq("name", nameOrUuid) ))));
    }

    public Nexus findByOwnerAndUuid(Account account, String uuid) {
        return findByUniqueFields("owner", account.getUuid(), "uuid", uuid);
    }

}
