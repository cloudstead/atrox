package histori.dao;

import histori.dao.search.SuperNexusIterator;
import histori.model.Account;
import histori.model.CanonicalEntity;
import histori.model.Nexus;
import histori.model.SuperNexus;
import histori.model.support.EntityVisibility;
import histori.model.support.GeoBounds;
import histori.model.support.GlobalSortOrder;
import histori.model.support.TimeRange;
import org.cobbzilla.wizard.dao.shard.AbstractShardedDAO;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class SuperNexusDAO extends AbstractShardedDAO<SuperNexus, SuperNexusShardDAO> {

    public void updateSuperNexus(Nexus nexus) {
        final String name = nexus.getName();
        final String canonical = CanonicalEntity.canonicalize(name);
        SuperNexus sn;
        switch (nexus.getVisibility()) {
            case everyone:
                sn = findByUniqueFields("canonicalName", canonical, "visibility", EntityVisibility.everyone);
                if (sn == null) {
                    create(new SuperNexus(nexus));
                } else {
                    if (sn.update(nexus)) update(sn);
                }
                break;

            case deleted:
                sn = findByUniqueFields("canonicalName", canonical, "visibility", nexus.getVisibility(), "account", nexus.getOwner());
                if (sn != null) update(sn.setDirty());
                sn = findByUniqueFields("canonicalName", canonical, "visibility", EntityVisibility.everyone);
                if (sn != null) update(sn.setDirty());
                break;

            default:
                sn = findByUniqueFields("canonicalName", canonical, "visibility", nexus.getVisibility(), "account", nexus.getOwner());
                if (sn == null) {
                    create(new SuperNexus(nexus));
                } else {
                    if (sn.update(nexus)) update(sn);
                }
                break;
        }
    }

    /**
     * Search SuperNexuses, return Iterator of names
     * @param range The time Range
     * @param bounds The geographic bounds
     * @param account The caller's account
     * @param visibility The visibility level
     * @param sort The sort order
     * @return an Iterator of SuperNexus names
     */
    public List<SuperNexusIterator> findNames(TimeRange range, GeoBounds bounds, Account account, EntityVisibility visibility, GlobalSortOrder sort) {
        final List<SuperNexusIterator> iterators = new ArrayList<>();
        for (SuperNexusShardDAO dao : getNonOverlappingDAOs()) {
            iterators.add(new SuperNexusIterator(dao, range, bounds, account, visibility, sort));
        }
        return iterators;
    }

}
