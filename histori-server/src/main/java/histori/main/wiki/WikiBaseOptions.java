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

    public static final String USAGE_OUTPUT_DIR = "Output directory";
    public static final String OPT_OUTPUT_DIR = "-o";
    public static final String LONGOPT_OUTPUT_DIR= "--output-dir";
    @Option(name=OPT_OUTPUT_DIR, aliases=LONGOPT_OUTPUT_DIR, usage=USAGE_OUTPUT_DIR, required=true)
    @Getter @Setter private File outputDir;

    public AssetStorageService getStorageService() {

        final Map<String, String> config = new HashMap<>();
        config.put(LocalAssetStorageService.PROP_BASE, abs(outputDir));

        final LocalAssetStorageService service = new LocalAssetStorageService(config);

        // ensures that LocalAssetStorate does not write out .contentType companion files for every stored file
        service.setContentType("application/json");

        return service;
    }

    public WikiArchive getWikiArchive() { return new WikiArchive(getStorageService()); }

}
