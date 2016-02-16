package histori.model.support;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Embeddable;

@Embeddable
public class VoteSummary {

    @Getter @Setter private int upVotes;
    @Getter @Setter private int downVotes;

}
