package histori.dao;

import histori.dao.shard.PreferredOwnerShardDAO;
import histori.model.PreferredOwner;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

@Repository
public class PreferredOwnerDAO extends SpecialAuthorDAO<PreferredOwner, PreferredOwnerShardDAO> {

    @Override public ShardSetConfiguration getShardConfiguration() { return database.getShard("preferred-owner"); }

    @Override protected List<PreferredOwner> sort(List<PreferredOwner> list) {
        Collections.sort(list, PreferredOwner.SORT_PRIORITY);
        return list;
    }

    @Override protected boolean addSelf() { return true; }

    @Override protected String specialAuthorField() { return "preferred"; }

}
