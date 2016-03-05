package histori.dao;

import histori.model.Account;
import histori.model.Nexus;
import histori.model.NexusTag;
import histori.model.TagType;
import histori.model.support.EntityVisibility;
import histori.model.support.GeoBounds;
import histori.model.support.TimeRange;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.math.BigInteger;
import java.util.List;

import static org.hibernate.criterion.Restrictions.*;

@Repository @Slf4j
public class NexusDAO extends VersionedEntityDAO<Nexus> {

    public static final int MAX_RESULTS = 200;

    @Autowired private NexusTagDAO nexusTagDAO;
    @Autowired private TagTypeDAO tagTypeDAO;
    @Autowired private RedisService redisService;

    @Getter(lazy=true) private final RedisService filterCache = initFilterCache();
    private RedisService initFilterCache() { return redisService.prefixNamespace(NexusEntityFilter.class.getSimpleName(), null); }

    @Override public Object preCreate(@Valid Nexus entity) {
        entity.prepareForSave();

        // ensure tag is present, or create it if not
        if (entity.hasNexusType()) {
            return new NexusTag().setTagName(entity.getNexusType()).setTagType(TagType.EVENT_TYPE);
        }

        return super.preCreate(entity);
    }

    @Override public Object preUpdate(@Valid Nexus entity) {
        entity.prepareForSave();

        // ensure event_type tag corresponding to nexusType is present, or create it if not
        if (entity.hasNexusType()) {
            // what tags already exist?
            final List<NexusTag> nexusTags = nexusTagDAO.findByNexusAndOwner(entity.getOwner(), entity.getUuid());
            entity.setTags(nexusTags);

            final NexusTag typeTag = (NexusTag) new NexusTag().setTagName(entity.getNexusType()).setTagType(TagType.EVENT_TYPE);
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

    @Override public Nexus postCreate(Nexus entity, Object context) {
        createEventTypeTag(entity, context);
        return super.postCreate(entity, context);
    }

    @Override public Nexus postUpdate(@Valid Nexus entity, Object context) {
        createEventTypeTag(entity, context);
        return super.postUpdate(entity, context);
    }

    public void createEventTypeTag(Nexus entity, Object context) {
        if (context instanceof NexusTag) {
            final NexusTag tag = (NexusTag) context;
            // create the event_type tag
            final NexusTag typeTag = (NexusTag) tag.setNexus(entity.getUuid()).setOwner(entity.getOwner());
            if (!entity.hasExactTag(typeTag)) {
                nexusTagDAO.create(typeTag);
            }
        }
    }

    public List<Nexus> findByName(String name) { return findByField("name", name); }

    public Nexus findByOwnerAndName(Account account, String name) {
        return uniqueResult(criteria().add(and(
                eq("owner", account.getUuid()),
                eq("name", name))));
    }

    public Nexus findByOwnerAndNameOrUuid(Account account, String nameOrUuid) {
        return uniqueResult(criteria().add(and(
                eq("owner", account.getUuid()),
                or( eq("uuid", nameOrUuid),
                        eq("name", nameOrUuid) ))));
    }

    public Nexus findByOwnerAndUuid(Account account, String uuid) {
        return uniqueResult(criteria().add(and(
                eq("owner", account.getUuid()),
                eq("uuid", uuid))));
    }

    /**
     * Find all publicly-viewable nexus in the range
     * @param range the time range to search
     * @param bounds the geo bounds for the search
     * @param query a tag query
     * @return a List of Nexus objects
     */
    public List<Nexus> findByTimeRangeAndGeo(TimeRange range, GeoBounds bounds, final String query) {
        final BigInteger start = range.getStartPoint().getInstant();
        final BigInteger end = range.getEndPoint().getInstant();
        return list(criteria().add(and(
                    or(
                            and(ge("timeRange.startPoint.instant", start), le("timeRange.startPoint.instant", end)),
                            and(ge("timeRange.endPoint.instant", start), le("timeRange.endPoint.instant", end))),
                    boundsClause(bounds),
                    eq("visibility", EntityVisibility.everyone)
            )).addOrder(Order.desc("timeRange.startPoint.instant")), 0, MAX_RESULTS, getFilter(query));
    }

    /**
     * Find all nexus in the range that are owned by the account
     * @param account the Nexus owner
     * @param range the time range to search
     * @param visibility what kinds of nexuses to return
     * @param query a tag query
     * @return a List of Nexus objects
     */
    public List<Nexus> findByTimeRangeAndGeo(Account account, TimeRange range, GeoBounds bounds, EntityVisibility visibility, String query) {
        final BigInteger start = range.getStartPoint().getInstant();
        final BigInteger end = range.getEndPoint().getInstant();
        return list(criteria().add(and(
                    or(
                            and(ge("timeRange.startPoint.instant", start), le("timeRange.startPoint.instant", end)),
                            and(ge("timeRange.endPoint.instant", start), le("timeRange.endPoint.instant", end))),
                    boundsClause(bounds),
                    visibilityClause(account, visibility))
            ).addOrder(Order.desc("timeRange.startPoint.instant")), 0, MAX_RESULTS, getFilter(query));
    }

    protected NexusEntityFilter getFilter(String query) {
        return new NexusEntityFilter(query, getFilterCache(), nexusTagDAO);
    }

    public Criterion visibilityClause(Account account, EntityVisibility visibility) {
        if (account == null) return eq("visibility", EntityVisibility.everyone);
        switch (visibility) {
            case everyone:       return or(
                    and(eq("owner", account.getUuid()), eq("visibility", EntityVisibility.owner)),
                    eq("visibility", EntityVisibility.everyone));
            case owner: default: return and(eq("owner", account.getUuid()), eq("visibility", EntityVisibility.owner));
            case hidden:         return and(eq("owner", account.getUuid()), eq("visibility", EntityVisibility.hidden));
        }
    }

    public Criterion boundsClause(GeoBounds bounds) {
        return or(
                // check if northeast or northwest corner is within bounds
                and(le("bounds.north", bounds.getNorth()), ge("bounds.north", bounds.getSouth()),
                        or(and(le("bounds.east", bounds.getEast()), ge("bounds.east", bounds.getWest())),
                                and(le("bounds.west", bounds.getEast()), ge("bounds.west", bounds.getWest())))),

                // check if southeast or southwest corner is within bounds
                and(le("bounds.south", bounds.getNorth()), ge("bounds.south", bounds.getSouth()),
                        or(and(le("bounds.east", bounds.getEast()), ge("bounds.east", bounds.getWest())),
                                and(le("bounds.west", bounds.getEast()), ge("bounds.west", bounds.getWest()))))
        );
    }

}
