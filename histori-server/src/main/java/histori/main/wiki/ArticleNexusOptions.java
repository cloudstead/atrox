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

public class ArticleNexusOptions extends BaseMainOptions {

    public static final String USAGE_FILE = "Input: WikiArticle json file, or directory of json files";
    public static final String OPT_FILE = "-i";
    public static final String LONGOPT_FILE= "--input-file";
    @Option(name=OPT_FILE, aliases=LONGOPT_FILE, usage=USAGE_FILE, required=true)
    @Getter @Setter private File file = null;

    public static final String USAGE_OUTPUT_DIR = "Output directory. Default is to print to stdout";
    public static final String OPT_OUTPUT_DIR = "-o";
    public static final String LONGOPT_OUTPUT_DIR= "--output-dir";
    @Option(name=OPT_OUTPUT_DIR, aliases=LONGOPT_OUTPUT_DIR, usage=USAGE_OUTPUT_DIR)
    @Getter @Setter private File outputDir = null;

    public static final String USAGE_WIKI_DIR = "Base directory for wiki archive";
    public static final String OPT_WIKI_DIR = "-w";
    public static final String LONGOPT_WIKI_DIR= "--wiki-dir";
    @Option(name=OPT_WIKI_DIR, aliases=LONGOPT_WIKI_DIR, usage=USAGE_WIKI_DIR, required=true)
    @Getter @Setter private File wikiDir;

    public AssetStorageService getStorageService() {
        final Map<String, String> config = new HashMap<>();
        config.put(LocalAssetStorateService.PROP_BASE, abs(wikiDir));
        return new LocalAssetStorateService(config);
    }

    public WikiArchive getWikiArchive() { return new WikiArchive(getStorageService()); }

}
