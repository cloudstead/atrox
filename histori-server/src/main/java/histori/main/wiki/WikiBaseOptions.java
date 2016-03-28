package histori.main.wiki;

import cloudos.service.asset.AssetStorageService;
import cloudos.service.asset.LocalAssetStorageService;
import cloudos.service.asset.S3AssetStorageService;
import histori.wiki.WikiArchive;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.abs;

public class WikiBaseOptions extends BaseMainOptions {

    public static final String USAGE_PLACES_API_KEY = "Google Places API Key";
    public static final String OPT_PLACES_API_KEY = "-P";
    public static final String LONGOPT_PLACES_API_KEY= "--places-api-key";
    @Option(name=OPT_PLACES_API_KEY, aliases=LONGOPT_PLACES_API_KEY, usage=USAGE_PLACES_API_KEY)
    @Getter @Setter private String placesApiKey;

    public static final String WIKI_S3_ACCESS = "WIKI_S3_ACCESS";
    public static final String WIKI_S3_SECRET = "WIKI_S3_SECRET";

    public static final String USAGE_WIKI_DIR = "Base directory for wiki archive. Format may be filesystem path or s3:bucket/prefix/path/ (use "+WIKI_S3_ACCESS+" and "+WIKI_S3_SECRET+" env vars to set credentials)";
    public static final String OPT_WIKI_DIR = "-w";
    public static final String LONGOPT_WIKI_DIR= "--wiki-dir";
    @Option(name=OPT_WIKI_DIR, aliases=LONGOPT_WIKI_DIR, usage=USAGE_WIKI_DIR, required=true)
    @Getter @Setter private String wikiDir;

    public static final String USAGE_OVERWRITE = "Overwrite output files. Default is to preserve files.";
    public static final String OPT_OVERWRITE = "-O";
    public static final String LONGOPT_OVERWRITE= "--overwrite";
    @Option(name=OPT_OVERWRITE, aliases=LONGOPT_OVERWRITE, usage=USAGE_OVERWRITE)
    @Getter @Setter private boolean overwrite = false;

    private final Pattern s3pathPattern = Pattern.compile("s3:([-\\w]{0,62}\\w)/(.+)");
    public AssetStorageService getStorageService() {

        final AssetStorageService service;
        final Map<String, String> config = new HashMap<>();

        if (wikiDir.startsWith("s3:")) {
            final Matcher matcher = s3pathPattern.matcher(wikiDir);
            if (!matcher.find()) die("invalid s3 path:"+wikiDir);

            final String access = System.getenv(WIKI_S3_ACCESS);
            final String secret = System.getenv(WIKI_S3_SECRET);
            if (empty(access) || empty(secret)) die("env var not found: "+WIKI_S3_ACCESS + " and/or "+WIKI_S3_SECRET);

            config.put(S3AssetStorageService.PROP_ACCESS_KEY, access);
            config.put(S3AssetStorageService.PROP_SECRET_KEY, secret);
            config.put(S3AssetStorageService.PROP_BUCKET, matcher.group(1));
            config.put(S3AssetStorageService.PROP_PREFIX, matcher.group(2));
            config.put(S3AssetStorageService.PROP_LOCAL_CACHE, System.getenv("WIKI_S3_LOCAL_CACHE"));
            service = new S3AssetStorageService(config);

        } else {
            config.put(LocalAssetStorageService.PROP_BASE, abs(wikiDir));

            service = new LocalAssetStorageService(config);

            // ensures that LocalAssetStorate does not write out .contentType companion files for every stored file
            ((LocalAssetStorageService)service).setContentType("application/json");

        }
        return service;
    }

    public WikiArchive getWikiArchive() { return new WikiArchive(getStorageService(), getPlacesApiKey()); }

}
