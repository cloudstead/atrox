package atrox.model.support;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TagOrder {

    newest, oldest, highest_vote_total, most_upvotes, most_downvotes;

    @JsonCreator public static TagOrder create (String val) {
        try {
            return TagOrder.valueOf(val.toLowerCase());
        } catch (RuntimeException e) {
            return TagOrder.most_upvotes;
        }
    }

}
