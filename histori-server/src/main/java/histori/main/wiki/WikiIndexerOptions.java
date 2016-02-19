package histori.main.wiki;

import cloudos.service.asset.AssetStorageService;
import cloudos.service.asset.LocalAssetStorateService;
import histori.wiki.WikiArchive;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.cobbzilla.util.io.FileUtil.abs;

public class WikiIndexerOptions extends BaseMainOptions {

    public static final String USAGE_SKIP = "How many pages to skip at the start";
    public static final String OPT_SKIP = "-k";
    public static final String LONGOPT_SKIP= "--skip-pages";
    @Option(name=OPT_SKIP, aliases=LONGOPT_SKIP, usage=USAGE_SKIP)
    @Getter @Setter private int skipPages = 0;

    public static final String USAGE_OUTPUT_DIR = "Output directory";
    public static final String OPT_OUTPUT_DIR = "-o";
    public static final String LONGOPT_OUTPUT_DIR= "--output-dir";
    @Option(name=OPT_OUTPUT_DIR, aliases=LONGOPT_OUTPUT_DIR, usage=USAGE_OUTPUT_DIR, required=true)
    @Getter @Setter private File outputDir;

    public AssetStorageService getStorageService() {
        final Map<String, String> config = new HashMap<>();
        config.put(LocalAssetStorateService.PROP_BASE, abs(outputDir));
        return new LocalAssetStorateService(config);
    }

    public WikiArchive getWikiArchive() { return new WikiArchive(getStorageService()); }

}
