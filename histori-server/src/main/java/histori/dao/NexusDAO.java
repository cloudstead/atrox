package histori.dao;

import histori.model.Account;
import histori.model.Nexus;
import histori.model.NexusTag;
import histori.model.TagType;
import histori.model.support.EntityVisibility;
import histori.model.support.GeoBounds;
import histori.model.support.TimeRange;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.math.BigInteger;
import java.util.List;

import static org.hibernate.criterion.Restrictions.*;

@Repository
public class NexusDAO extends VersionedEntityDAO<Nexus> {

    public static final int MAX_RESULTS = 40;

    @Autowired private NexusTagDAO nexusTagDAO;

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

        // what tags already exist?
        final List<NexusTag> nexusTags = nexusTagDAO.findByNexusAndOwner(entity.getOwner(), entity.getUuid());

        // ensure event_type tag corresponding to nexusType is present, or create it if not
        if (entity.hasNexusType()) {
            if (!NexusTag.containsEventTypeTag(nexusTags, TagType.EVENT_TYPE)) {
                return new NexusTag().setTagName(entity.getNexusType()).setTagType(TagType.EVENT_TYPE);
            } else {
                // nexusType already matches one of the event_type tags
            }
        } else {
            final List<NexusTag> eventTypeTags = NexusTag.filterByType(nexusTags, TagType.EVENT_TYPE);
            if (!eventTypeTags.isEmpty()) {
                entity.setNexusType(eventTypeTags.get(0).getTagName());
            }
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
            nexusTagDAO.create((NexusTag) tag.setNexus(entity.getUuid()).setOwner(entity.getOwner()));
        }
    }

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
     * @return a List of Nexus objects
     */
    public List<Nexus> findByTimeRange(TimeRange range, GeoBounds bounds) {
        final BigInteger start = range.getStartPoint().getInstant();
        final BigInteger end = range.getEndPoint().getInstant();
        return list(criteria().add(and(
                or(
                        and(ge("timeRange.startPoint.instant", start), le("timeRange.startPoint.instant", end)),
                        and(ge("timeRange.endPoint.instant", start), le("timeRange.endPoint.instant", end))),
                boundsClause(bounds),
                eq("visibility", EntityVisibility.everyone)
        )).addOrder(Order.desc("timeRange.startPoint.instant")), 0, MAX_RESULTS);
    }

    /**
     * Find all nexus in the range that are owned by the account
     * @param account the Nexus owner
     * @param range the time range to search
     * @param visibility what kinds of nexuses to return
     * @return a List of Nexus objects
     */
    public List<Nexus> findByTimeRange(Account account, TimeRange range, GeoBounds bounds, EntityVisibility visibility) {
        final BigInteger start = range.getStartPoint().getInstant();
        final BigInteger end = range.getEndPoint().getInstant();
        return list(criteria().add(and(
                or(
                        and(ge("timeRange.startPoint.instant", start), le("timeRange.startPoint.instant", end)),
                        and(ge("timeRange.endPoint.instant", start), le("timeRange.endPoint.instant", end))),
                boundsClause(bounds),
                visibilityClause(account, visibility))
        ).addOrder(Order.desc("timeRange.startPoint.instant")), 0, MAX_RESULTS);
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
