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

    public List<Nexus> findByTimeRange(Account account, TimeRange range) {
        final BigInteger start = range.getStartPoint().getInstant();
        final BigInteger end = range.getEndPoint().getInstant();
        if (account != null) {
            return list(criteria().add(and(
                    or(le("timeRange.startPoint.instant", end), ge("timeRange.endPoint.instant", start)),
                    or(eq("owner", account.getUuid()), eq("visibility", EntityVisibility.everyone))
            )).addOrder(Order.desc("timeRange.startPoint.instant")), 0, 100);
        } else {
            return list(criteria().add(and(
                    or(le("timeRange.startPoint.instant", end), ge("timeRange.endPoint.instant", start)),
                    eq("visibility", EntityVisibility.everyone)
            )).addOrder(Order.desc("timeRange.startPoint.instant")), 0, 100);
        }
    }
}
