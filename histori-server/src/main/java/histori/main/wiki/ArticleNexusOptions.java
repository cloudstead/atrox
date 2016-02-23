package histori.main.wiki;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import java.io.File;

public class ArticleNexusOptions extends WikiBaseOptions {

    public static final String USAGE_FILE = "Input: WikiArticle json file, or directory of json files. Default is to read a list of files or article names from stdin";
    public static final String OPT_FILE = "-i";
    public static final String LONGOPT_FILE= "--input-file";
    @Option(name=OPT_FILE, aliases=LONGOPT_FILE, usage=USAGE_FILE)
    @Getter @Setter private String input = null;

    public static final String USAGE_OUTPUT_DIR = "Output directory. Default is to print to stdout.";
    public static final String OPT_OUTPUT_DIR = "-o";
    public static final String LONGOPT_OUTPUT_DIR= "--output-dir";
    @Option(name=OPT_OUTPUT_DIR, aliases=LONGOPT_OUTPUT_DIR, usage=USAGE_OUTPUT_DIR)
    @Getter @Setter private File outputDir;

    public static final String USAGE_OVERWRITE = "Overwrite output files. Default is to preserve files.";
    public static final String OPT_OVERWRITE = "-O";
    public static final String LONGOPT_OVERWRITE= "--overwrite";
    @Option(name=OPT_OVERWRITE, aliases=LONGOPT_OVERWRITE, usage=USAGE_OVERWRITE)
    @Getter @Setter private boolean overwrite = false;

    public static final String USAGE_ERROR_LOG = "Error log. Articles that cannot be parsed will be written here, one per line.";
    public static final String OPT_ERROR_LOG = "-E";
    public static final String LONGOPT_ERROR_LOG= "--error-log";
    @Option(name=OPT_ERROR_LOG, aliases=LONGOPT_ERROR_LOG, usage=USAGE_ERROR_LOG)
    @Getter @Setter private File errorLog;
    public boolean hasErrorLog () { return errorLog != null; }

}
