package histori.dao;

import histori.model.Account;
import histori.model.Vote;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository public class VoteDAO extends VersionedEntityDAO<Vote> {

    public List<Vote> findByEntity(String uuid) { return findByField("entity", uuid); }
    public List<Vote> findByOwner (String uuid) { return findByField("owner", uuid); }

    public Vote findByOwnerAndEntity(Account account, String uuid) {
        return findByUniqueFields("owner", account.getUuid(), "entity", uuid);
    }

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

}
