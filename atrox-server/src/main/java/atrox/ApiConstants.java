package atrox;

import org.apache.commons.lang3.RandomStringUtils;

public class ApiConstants {

    // must match what is in api.js
    public static final String API_TOKEN = "x-atrox-api-key";

    public static final String ACCOUNTS_ENDPOINT = "/accounts";

    public static final String ERR_ALREADY_LOGGED_IN = "err.alreadyLoggedIn";

    public static final String PLACEHOLDER_MOBILE_PHONE = "8005550100";
    public static final String ANONYMOUS_EMAIL = "anonymous-#STAMP#@example.com";

    // auth endpoints
    public static final String EP_REGISTER = "/register";
    public static final String EP_LOGIN = "/login";

    public static String anonymousEmail() {
        return ANONYMOUS_EMAIL.replace("#STAMP#", RandomStringUtils.randomAlphanumeric(10)+"-"+System.currentTimeMillis());
    }
}
