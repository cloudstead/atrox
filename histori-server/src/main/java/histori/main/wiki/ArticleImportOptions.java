package histori.main.wiki;

import histori.wiki.WikiArchive;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;

import java.io.File;

public class ArticleImportOptions extends BaseMainOptions {

    public static final String USAGE_WDIR = "Directory containing Wikipedia index and article files";
    public static final String OPT_WDIR = "-w";
    public static final String LONGOPT_WDIR= "--wiki-dir";
    @Option(name=OPT_WDIR, aliases=LONGOPT_WDIR, usage=USAGE_WDIR, required=true)
    @Getter @Setter private File wikiDir = null;

    @Getter(lazy=true) private final WikiArchive wiki = initArchive();
    private WikiArchive initArchive() { return new WikiArchive(wikiDir); }

    public static final String USAGE_FILE = "Input WikiArticle json file";
    public static final String OPT_FILE = "-i";
    public static final String LONGOPT_FILE= "--input-file";
    @Option(name=OPT_FILE, aliases=LONGOPT_FILE, usage=USAGE_FILE, required=true)
    @Getter @Setter private File file = null;

    public static final String USAGE_OUTPUT_DIR = "Output directory. Default is to print to stdout";
    public static final String OPT_OUTPUT_DIR = "-o";
    public static final String LONGOPT_OUTPUT_DIR= "--output-dir";
    @Option(name=OPT_OUTPUT_DIR, aliases=LONGOPT_OUTPUT_DIR, usage=USAGE_OUTPUT_DIR)
    @Getter @Setter private File outputDir = null;

}
