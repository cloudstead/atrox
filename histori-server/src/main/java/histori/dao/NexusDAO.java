package histori.dao;

import histori.model.Account;
import histori.model.Nexus;
import histori.model.support.EntityVisibility;
import histori.model.support.TimeRange;
import org.hibernate.criterion.Order;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.math.BigInteger;
import java.util.List;

import static org.hibernate.criterion.Restrictions.*;

@Repository
public class NexusDAO extends VersionedEntityDAO<Nexus> {

    public static final int MAX_RESULTS = 40;

    @Override public Object preCreate(@Valid Nexus entity) {
        entity.initTimeInstants();
        return super.preCreate(entity);
    }

    @Override public Object preUpdate(@Valid Nexus entity) {
        entity.initTimeInstants();
        return super.preUpdate(entity);
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
     * @return a List of Nexus objects
     */
    public List<Nexus> findByTimeRange(TimeRange range) {
        final BigInteger start = range.getStartPoint().getInstant();
        final BigInteger end = range.getEndPoint().getInstant();
        return list(criteria().add(and(
                or(
                        and(ge("timeRange.startPoint.instant", start), le("timeRange.startPoint.instant", end)),
                        and(ge("timeRange.endPoint.instant", start), le("timeRange.endPoint.instant", end))),
                eq("visibility", EntityVisibility.everyone)
        )).addOrder(Order.desc("timeRange.startPoint.instant")), 0, MAX_RESULTS);
    }

    /**
     * Find all nexus in the range that are owned by the account
     * @param account the Nexus owner
     * @param range the time range to search
     * @return a List of Nexus objects
     */
    public List<Nexus> findByTimeRange(Account account, TimeRange range) {
        final BigInteger start = range.getStartPoint().getInstant();
        final BigInteger end = range.getEndPoint().getInstant();
        return list(criteria().add(and(
                or(
                        and(ge("timeRange.startPoint.instant", start), le("timeRange.startPoint.instant", end)),
                        and(ge("timeRange.endPoint.instant", start), le("timeRange.endPoint.instant", end))),
                or(eq("owner", account.getUuid()), eq("visibility", EntityVisibility.everyone)))
        ).addOrder(Order.desc("timeRange.startPoint.instant")), 0, MAX_RESULTS);
    }

}
