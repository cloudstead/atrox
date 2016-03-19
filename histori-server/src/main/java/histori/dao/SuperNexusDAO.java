package histori.dao;

import histori.dao.shard.SuperNexusShardDAO;
import histori.model.Nexus;
import histori.model.SuperNexus;
import histori.model.support.EntityVisibility;
import org.cobbzilla.wizard.model.shard.ShardIO;
import org.cobbzilla.wizard.server.config.DatabaseConfiguration;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import static histori.model.CanonicalEntity.canonicalize;

@Repository
public class SuperNexusDAO extends ShardedEntityDAO<SuperNexus, SuperNexusShardDAO> {

    @Autowired private DatabaseConfiguration database;
    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("super-nexus"); }

    @Transactional
    public void updateSuperNexus(Nexus nexus) {
        final String name = nexus.getName();
        final String canonical = canonicalize(name);
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

    public void forceRefresh () {
        for (SuperNexusShardDAO dao : getDAOs(ShardIO.read)) {
            dao.forceRefresh();
        }
    }

    public long oldestRefreshTime () {
        long oldest = Long.MAX_VALUE;
        for (SuperNexusShardDAO dao : getDAOs(ShardIO.read)) {
            long lastRefresh = dao.getLastRefresh();
            if (lastRefresh < oldest) oldest = lastRefresh;
        }
        return oldest;
    }
}
