package atrox;

import atrox.model.canonical.*;
import atrox.model.history.*;
import atrox.model.internal.EntityPointer;
import atrox.model.tag.CitationTag;
import atrox.model.tag.IdeaTag;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.collection.ArrayUtil;
import org.cobbzilla.util.string.StringUtil;

import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.reflect.ReflectionUtil.forName;

public class ApiConstants {

    // must match what is in api.js
    public static final String API_TOKEN = "x-atrox-api-key";

    public static final String SYSTEM_UUID = "_system_";

    public static final String ACCOUNTS_ENDPOINT = "/accounts";
    public static final String AUTOCOMPLETE_ENDPOINT = "/autocomplete";
    public static final String MAP_IMAGES_ENDPOINT = "/map_images";
    public static final String HISTORIES_ENDPOINT = "/histories";

    public static final int ENTITY_TYPE_MAXLEN = 100;

    public static String historyEndpoint(Class clazz) {
        if (EntityHistory.class.isAssignableFrom(clazz)) {
            return HISTORIES_ENDPOINT + "/" + clazz.getSimpleName().replace("History", "");
        }
        return HISTORIES_ENDPOINT + "/" + clazz.getSimpleName();
    }

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

    // histories resource sub-endpoints
    public static final String EP_CANONICAL = "/canonical";
    public static final String EP_BY_ID = "/id";
    public static final String EP_BY_DATE = "/date";
    public static final String EP_AUTOCOMPLETE = "/autocomplete";

    // bound used when searching by date
    public static final String BOUND_RANGE = "range";
    public static final String RANGE_SEP = "|";

    public static String anonymousEmail() {
        return ANONYMOUS_EMAIL.replace("#STAMP#", RandomStringUtils.randomAlphanumeric(10)+"-"+System.currentTimeMillis());
    }

    public static final Class[] CANONICAL_ENTITIES = {
            Citation.class, Idea.class,
            WorldActor.class, WorldEvent.class, EventActor.class,
            ImpactType.class, EventImpact.class,
            IncidentType.class, EventIncident.class
    };

    public static final Class[] HISTORY_ENTITIES = {
            CitationHistory.class, IdeaHistory.class,
            WorldActorHistory.class, WorldEventHistory.class, EventActorHistory.class,
            ImpactTypeHistory.class, EventImpactHistory.class,
            IncidentTypeHistory.class, EventIncidentHistory.class
    };

    public static final Class[] TAG_ENTITIES = { CitationTag.class, IdeaTag.class };

    public static final Class[] ALL_ENTITIES
            = ArrayUtil.concat(CANONICAL_ENTITIES, HISTORY_ENTITIES, TAG_ENTITIES, new Class[]{EntityPointer.class} );

    public static final Map<String, Class> ENTITY_CLASS_MAP = new HashMap<>();
    public static final Map<String, Class> CANONICAL_ENTITY_CLASS_MAP = new HashMap<>();
    static {
        buildClassMap(CANONICAL_ENTITIES, CANONICAL_ENTITY_CLASS_MAP);
        buildClassMap(ALL_ENTITIES, ENTITY_CLASS_MAP);
        ENTITY_CLASS_MAP.put("any", EntityPointer.class);
    }

    private static void buildClassMap(Class[] classes, Map<String, Class> map) {
        for (Class c : classes) {
            map.put(c.getSimpleName(), c);
            map.put(StringUtil.uncapitalize(c.getSimpleName()), c);
            map.put(c.getName(), c);
        }
    }

    public static Class<? extends EntityHistory> getHistoryClass(Class c) {
        if (c.getName().endsWith("History")) return c;
        return forName(c.getName().replace(".canonical.", ".history.") + "History");
    }
}
