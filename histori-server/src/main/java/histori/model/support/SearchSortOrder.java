package histori.model.support;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.sun.xml.internal.ws.api.message.ExceptionHasMessage;

public enum SearchSortOrder {

    newest, oldest, up_vote, down_vote, vote_tally, vote_count;

    @JsonCreator public static SearchSortOrder create (String val) { return valueOf(val.toLowerCase()); }

    public static SearchSortOrder valueOf(String val, SearchSortOrder defaultValue) {
        try { return create(val); } catch (Exception ignored) {}
        return defaultValue;
    }

}
