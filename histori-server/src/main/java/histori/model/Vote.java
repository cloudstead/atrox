package histori.model;

import histori.model.base.VoteBase;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity @Slf4j
@NoArgsConstructor @Accessors(chain=true)
@Table(uniqueConstraints = @UniqueConstraint(name = "vote_uniq", columnNames = {"owner", "entity"}))
public class Vote extends VoteBase {

    public static Vote upVote (String account, String entity) { return new Vote(account, entity, 1); }

    public static Vote downVote (String account, String entity) { return new Vote(account, entity, -1); }

    public Vote(String account, String entity, int vote) {
        setOwner(account);
        setEntity(entity);
        setVote((short) (vote >= 1 ? 1 : -1));
    }

}
