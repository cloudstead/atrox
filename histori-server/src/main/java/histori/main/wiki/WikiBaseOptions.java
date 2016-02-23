package histori.main.wiki;

import cloudos.service.asset.AssetStorageService;
import cloudos.service.asset.LocalAssetStorageService;
import histori.wiki.WikiArchive;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.io.FileUtil.abs;

public class WikiBaseOptions extends BaseMainOptions {

    public static final String USAGE_PLACES_API_KEY = "Google Places API Key";
    public static final String OPT_PLACES_API_KEY = "-P";
    public static final String LONGOPT_PLACES_API_KEY= "--places-api-key";
    @Option(name=OPT_PLACES_API_KEY, aliases=LONGOPT_PLACES_API_KEY, usage=USAGE_PLACES_API_KEY)
    @Getter @Setter private String placesApiKey;

    public static final String USAGE_WIKI_DIR = "Base directory for wiki archive";
    public static final String OPT_WIKI_DIR = "-w";
    public static final String LONGOPT_WIKI_DIR= "--wiki-dir";
    @Option(name=OPT_WIKI_DIR, aliases=LONGOPT_WIKI_DIR, usage=USAGE_WIKI_DIR, required=true)
    @Getter @Setter private File wikiDir;

    public AssetStorageService getStorageService() {

        final Map<String, String> config = new HashMap<>();
        config.put(LocalAssetStorageService.PROP_BASE, abs(wikiDir));

        final LocalAssetStorageService service = new LocalAssetStorageService(config);

        // ensures that LocalAssetStorate does not write out .contentType companion files for every stored file
        service.setContentType("application/json");

        return service;
    }

    public WikiArchive getWikiArchive() { return new WikiArchive(getStorageService(), getPlacesApiKey()); }

}
