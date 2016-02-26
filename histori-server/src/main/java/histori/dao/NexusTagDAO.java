package histori.dao;

import histori.model.Account;
import histori.model.Nexus;
import histori.model.NexusTag;
import histori.model.TagType;
import histori.model.support.EntityVisibility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

import static org.hibernate.criterion.Restrictions.*;

@Repository
public class NexusTagDAO extends VersionedEntityDAO<NexusTag> {

    @Autowired private NexusDAO nexusDAO;

    @Override public NexusTag postCreate(NexusTag entity, Object context) {
        if (entity.getTagType().equalsIgnoreCase(TagType.EVENT_TYPE)) {
            final Nexus nexus = nexusDAO.findByUuid(entity.getNexus());
            if (!nexus.hasNexusType()) nexus.setNexusType(entity.getTagName());
            nexusDAO.update(nexus);
        }
        return entity;
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
