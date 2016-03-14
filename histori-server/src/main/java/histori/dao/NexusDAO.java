package histori.dao;

import histori.model.Account;
import histori.model.Nexus;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.shard.AbstractShardedDAO;
import org.springframework.stereotype.Repository;

import java.util.List;

import static histori.model.CanonicalEntity.canonicalize;

@Repository @Slf4j
public class NexusDAO extends AbstractShardedDAO<Nexus, NexusShardDAO> {

    public Nexus findByOwnerAndName(Account account, String name) {
        return findByUniqueFields("owner", account, "canonicalName", canonicalize(name));
    }

    public List<Nexus> findByName(String name) {
        return findByField("canonicalName", canonicalize(name));
    }
}
