package histori.dao;

import edu.emory.mathcs.backport.java.util.Arrays;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.NexusTag;
import histori.model.TagType;
import histori.model.support.EntityVisibility;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.hibernate.criterion.Restrictions.*;

@Repository @Slf4j
public class NexusTagDAO extends VersionedEntityDAO<NexusTag> {

    @Autowired private RedisService redisService;
    @Autowired private NexusDAO nexusDAO;

    public static final long TAG_CACHE_TIMEOUT = TimeUnit.DAYS.toSeconds(1);
    @Getter(lazy=true) private final RedisService tagCache = initTagCache();
    private RedisService initTagCache() { return redisService.prefixNamespace("NexusTagDAO.findByNexus", null); }

    @Override public Object preCreate(@Valid NexusTag entity) {
        getTagCache().del(entity.getNexus());
        return super.preCreate(entity);
    }

    @Override public Object preUpdate(@Valid NexusTag entity) {
        getTagCache().del(entity.getNexus());
        return super.preUpdate(entity);
    }

    @Override public void delete(String uuid) {
        final NexusTag tag = findByUuid(uuid);
        if (tag != null) getTagCache().del(tag.getNexus());
        super.delete(uuid);
    }

    @Override public NexusTag postCreate(NexusTag nexusTag, Object context) {
        if (nexusTag.hasTagType()) {
            if (nexusTag.getTagType().equalsIgnoreCase(TagType.EVENT_TYPE)) {
                final Nexus nexus = nexusDAO.findByUuid(nexusTag.getNexus());
                if (!nexus.hasNexusType()) nexus.setNexusType(nexusTag.getTagName());
                nexusDAO.update(nexus);
            }
        }
        return nexusTag;
    }

    public NexusTag findByNexusAndOwnerAndName(String uuid, Account account, String tagName) {
        return uniqueResult(criteria().add(
                and(
                        eq("nexus", uuid),
                        eq("tagName", tagName),
                        eq("owner", account.getUuid()))));
    }

    /**
     * Find all tags for the nexus owned by the account
     * @param account the account who owns the tags
     * @param uuid id of the nexus
     * @return list of NexusTags
     */
    public List<NexusTag> findByNexusAndOwner(Account account, String uuid) {
        return findByNexusAndOwner(account.getUuid(), uuid);
    }

    public List<NexusTag> findByNexusAndOwner(String accountUuid, String nexusUuid) {
        return list(criteria().add(
                and(
                        eq("nexus", nexusUuid),
                        eq("owner", accountUuid))));
    }

    public List<NexusTag> findByNexus(String uuid) {
        List<NexusTag> tags = findCachedTags(uuid);
        if (tags == null) {
            tags = findByField("nexus", uuid);
            try {
                getTagCache().set(uuid, JsonUtil.toJson(tags), "EX", TAG_CACHE_TIMEOUT);
            } catch (Exception e) {
                log.warn("findByNexus: Error storing in cache: "+e);
            }
        }
        return tags;
    }

    private List<NexusTag> findCachedTags(String uuid) {
        try {
            return Arrays.asList(fromJson(getTagCache().get(uuid), NexusTag[].class));
        } catch (Exception e) {
            log.warn("findCachedTags: error reading from cache: "+e);
            return null;
        }
    }

    public List<NexusTag> findByNexusAndOwnerAndType(String accountUuid, String nexusUuid, String tagType) {
        return list(criteria().add(
                and(
                        eq("nexus", nexusUuid),
                        eq("tagType", tagType),
                        eq("owner", accountUuid))));
    }

    public List<NexusTag> findByNexus(Account account, String uuid, EntityVisibility visibility) {
        switch (visibility) {
            default:
            case everyone:
                if (account == null) {
                    // only return public stuff
                    return list(criteria().add(
                            and(
                                    eq("nexus", uuid),
                                    eq("visibility", EntityVisibility.everyone))));

                } else {
                    // return public stuff + anything owned by the caller that is not hidden
                    return list(criteria().add(
                            and(eq("nexus", uuid),
                                    or(
                                            eq("visibility", EntityVisibility.everyone),
                                            and(
                                                    eq("owner", account.getUuid()),
                                                    ne("visibility", EntityVisibility.hidden))
                                    ))));
                }

            case owner:
                // return anything owned by the caller that is not hidden
                if (account == null) return new ArrayList<>();
                return list(criteria().add(
                        and(
                                eq("nexus", uuid),
                                eq("owner", account.getUuid()),
                                ne("visibility", EntityVisibility.hidden))));

            case hidden:
                // return anything owned by the caller that is hidden
                if (account == null) return new ArrayList<>();
                return list(criteria().add(
                        and(
                                eq("nexus", uuid),
                                eq("owner", account.getUuid()),
                                eq("visibility", EntityVisibility.hidden))));
        }
    }

    public List<NexusTag> findByNexusAndName(Account account, String uuid, String tagName, EntityVisibility visibility) {
        switch (visibility) {
            default:
            case everyone:
                if (account == null) {
                    // only return public stuff
                    return list(criteria().add(
                            and(
                                    eq("nexus", uuid),
                                    eq("tagName", tagName),
                                    eq("visibility", EntityVisibility.everyone))));

                } else {
                    // return public stuff + anything owned by the caller that is not hidden
                    return list(criteria().add(
                            and(eq("nexus", uuid),
                                    eq("tagName", tagName),
                                    or(
                                            eq("visibility", EntityVisibility.everyone),
                                            and(
                                                    eq("owner", account.getUuid()),
                                                    ne("visibility", EntityVisibility.hidden))
                                    ))));
                }

            case owner:
                // return anything owned by the caller that is not hidden
                if (account == null) return new ArrayList<>();
                return list(criteria().add(
                        and(
                                eq("nexus", uuid),
                                eq("tagName", tagName),
                                eq("owner", account.getUuid()),
                                ne("visibility", EntityVisibility.hidden))));

            case hidden:
                // return anything owned by the caller that is hidden
                if (account == null) return new ArrayList<>();
                return list(criteria().add(
                        and(
                                eq("nexus", uuid),
                                eq("tagName", tagName),
                                eq("owner", account.getUuid()),
                                eq("visibility", EntityVisibility.hidden))));
        }
    }

}
