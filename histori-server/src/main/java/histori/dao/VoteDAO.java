package histori.dao;

import histori.model.Account;
import histori.model.Vote;
import org.cobbzilla.wizard.dao.AbstractCRUDDAO;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.hibernate.criterion.Restrictions.and;
import static org.hibernate.criterion.Restrictions.eq;

@Repository public class VoteDAO extends AbstractCRUDDAO<Vote> {

    public List<Vote> findByEntity(String uuid) { return findByField("entity", "uuid"); }
    public List<Vote> findByOwner (String uuid) { return findByField("owner", "uuid"); }

    public Vote upVote(Account account, String uuid) {
        final String accountUuid = account.getUuid();
        final Vote vote = Vote.upVote(accountUuid, uuid);
        return castVote(account, uuid, vote);
    }

    public Vote downVote(Account account, String uuid) {
        final String accountUuid = account.getUuid();
        final Vote vote = Vote.downVote(accountUuid, uuid);
        return castVote(account, uuid, vote);
    }

    public Vote castVote(Account account, String uuid, Vote vote) {
        final Vote existing = findByOwnerAndEntity(account, uuid);
        if (existing == null) return create(vote);
        existing.setVote(vote.getVote());
        return update(existing);
    }

    public Vote findByOwnerAndEntity(Account account, String uuid) {
        return uniqueResult(criteria().add(and(
                eq("owner", account.getUuid()),
                eq("entity", uuid))));
    }
}
