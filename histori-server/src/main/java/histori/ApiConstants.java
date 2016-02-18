package histori;

import org.apache.commons.lang3.RandomStringUtils;

public class ApiConstants {

    // must match what is in api.js
    public static final String API_TOKEN = "x-histori-api-key";

    public static final String SYSTEM_UUID = "_system_";

    public static final String ACCOUNTS_ENDPOINT = "/accounts";
    public static final String AUTOCOMPLETE_ENDPOINT = "/autocomplete";
    public static final String MAP_IMAGES_ENDPOINT = "/map_images";
    public static final String NEXUS_ENDPOINT = "/nexus";
    public static final String SEARCH_ENDPOINT = "/search";

    public static final int NAME_MAXLEN = 200;
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
    public static final String EP_DATE = "/date";

    // bound used when searching by date
    public static final String BOUND_RANGE = "range";
    public static final String RANGE_SEP = "|";

    public static String anonymousEmail() {
        return ANONYMOUS_EMAIL.replace("#STAMP#", RandomStringUtils.randomAlphanumeric(10)+"-"+System.currentTimeMillis());
    }

}
