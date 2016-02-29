package histori;

import histori.model.SocialEntity;
import org.apache.commons.lang3.RandomStringUtils;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;

public class ApiConstants {

    // must match what is in api.js
    public static final String API_TOKEN = "x-histori-api-key";

    public static final String SYSTEM_UUID = "_system_";

    public static final String ACCOUNTS_ENDPOINT = "/accounts";
    public static final String MAP_IMAGES_ENDPOINT = "/map_images";
    public static final String NEXUS_ENDPOINT = "/nexus";
    public static final String SEARCH_ENDPOINT = "/search";
    public static final String TAGS_ENDPOINT = "/tags";
    public static final String TAG_TYPES_ENDPOINT = "/tag_types";
    public static final String VOTES_ENDPOINT = "/votes";
    public static final String ARCHIVES_ENDPOINT = "/archives";

    public static final int NAME_MAXLEN = 1024;
    public static final int GEOJSON_MAXLEN = 64000;

    public static final String ERR_ALREADY_LOGGED_IN = "err.alreadyLoggedIn";

    public static final String PLACEHOLDER_MOBILE_PHONE = "8005550100";
    public static final String ANONYMOUS_EMAIL = "anonymous-#STAMP#@example.com";

    // auth endpoints
    public static final String EP_REGISTER = "/register";
    public static final String EP_LOGIN = "/login";

    // map image endpoints
    public static final String EP_PUBLIC = "/public";
    public static final String EP_GET_MAP_IMAGE = "/image";
    public static final String EP_TRANSFORM_MAP_IMAGE = "/transform";

    // nexus endpoints
    public static final String EP_TAGS = "/tags";

    // search endpoints
    public static final String EP_QUERY = "/q";
    public static final String EP_NEXUS = "/n";

    // tag endpoints and query params
    public static final String EP_TAG = "/tag";
    public static final String EP_AUTOCOMPLETE = "/autocomplete";
    public static final String EP_RESOLVE = "/resolve";
    public static final String EP_OWNER = "/owner";
    public static final String QPARAM_AUTOCOMPLETE = "q";
    public static final String MATCH_NULL_TYPE = "no-type";

    // bound used when searching by date
    public static final String BOUND_RANGE = "range";
    public static final String RANGE_SEP = "|";

    // voting endpoints
    public static final String EP_UPVOTE = "/up";
    public static final String EP_DOWNVOTE = "/down";
    public static final String EP_SUMMARY = "/summary";

    public static String anonymousEmail() {
        return ANONYMOUS_EMAIL.replace("#STAMP#", RandomStringUtils.randomAlphanumeric(10)+"-"+now());
    }

    public static String voteUri(SocialEntity entity) {
        return VOTES_ENDPOINT + "/" + entity.getClass().getSimpleName() + "/" + entity.getUuid();
    }
    public static String upvoteUri(SocialEntity entity) {
        return voteUri(entity) + EP_UPVOTE;
    }
    public static String downvoteUri(SocialEntity entity) {
        return voteUri(entity)+ EP_DOWNVOTE;
    }
}
