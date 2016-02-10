package atrox.model.support;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum EntitySearchOrder {

    newest, oldest, most_upvotes, most_downvotes;

    @JsonCreator public static EntitySearchOrder create (String val) {
        try {
            return EntitySearchOrder.valueOf(val.toLowerCase());
        } catch (RuntimeException e) {
            return EntitySearchOrder.most_upvotes;
        }
    }

}
