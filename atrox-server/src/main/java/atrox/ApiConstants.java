package atrox;

import org.apache.commons.lang3.RandomStringUtils;

public class ApiConstants {

    // must match what is in api.js
    public static final String API_TOKEN = "x-atrox-api-key";

    public static final String ACCOUNTS_ENDPOINT = "/accounts";
    public static final String NAMED_ENTITIES_ENDPOINT = "/map";

    public static final String ERR_ALREADY_LOGGED_IN = "err.alreadyLoggedIn";

    public static final String PLACEHOLDER_MOBILE_PHONE = "8005550100";
    public static final String ANONYMOUS_EMAIL = "anonymous-#STAMP#@example.com";

    // auth endpoints
    public static final String EP_REGISTER = "/register";
    public static final String EP_LOGIN = "/login";

    // world data model sub-endpoints
    public static final String EP_BY_DATE = "/by_date";
    public static final String EP_BY_NAME = "/by_name";
    public static final String EP_EDIT = "/edit";

    // bound used when searching by date
    public static final String BOUND_RANGE = "range";
    public static final String RANGE_SEP = "|";

    public static String anonymousEmail() {
        return ANONYMOUS_EMAIL.replace("#STAMP#", RandomStringUtils.randomAlphanumeric(10)+"-"+System.currentTimeMillis());
    }
}
