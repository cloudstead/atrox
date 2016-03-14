package histori.model.support;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum GlobalSortOrder {

    newest, oldest, up_vote, down_vote, vote_tally, vote_count;

    @JsonCreator public static GlobalSortOrder create (String val) { return valueOf(val.toLowerCase()); }

}
