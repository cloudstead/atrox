package histori.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum FeedPollInterval {

    hourly, daily;

    @JsonCreator public static FeedPollInterval fromString (String val) { return valueOf(val.toLowerCase()); }

}
