package atrox.main.wiki;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;

import java.io.File;

public class MasterDbSplitterOptions extends BaseMainOptions {

    public static final String USAGE_OUTPUT_DIR = "Output directory for split files. Default is current directory";
    public static final String OPT_OUTPUT_DIR = "-o";
    public static final String LONGOPT_OUTPUT_DIR= "--output-dir";
    @Option(name=OPT_OUTPUT_DIR, aliases=LONGOPT_OUTPUT_DIR, usage=USAGE_OUTPUT_DIR)
    @Getter @Setter private File outputDir = new File(System.getProperty("user.dir"));

    public static final String USAGE_OUTPUT_PREFIX = "Prefix for output files";
    public static final String OPT_OUTPUT_PREFIX = "-p";
    public static final String LONGOPT_OUTPUT_PREFIX= "--output-prefix";
    @Option(name=OPT_OUTPUT_PREFIX, aliases=LONGOPT_OUTPUT_PREFIX, usage=USAGE_OUTPUT_PREFIX)
    @Getter @Setter private String outputPrefix = "wikipedia-archive-";

    public static final String USAGE_SIZE = "How many articles to put into each file";
    public static final String OPT_SIZE = "-s";
    public static final String LONGOPT_SIZE= "--size";
    @Option(name=OPT_SIZE, aliases=LONGOPT_SIZE, usage=USAGE_SIZE)
    @Getter @Setter private int splitSize = 100_000;

    public static final String USAGE_SKIP = "How many pages to skip at the start";
    public static final String OPT_SKIP = "-k";
    public static final String LONGOPT_SKIP= "--skip-pages";
    @Option(name=OPT_SKIP, aliases=LONGOPT_SKIP, usage=USAGE_SKIP)
    @Getter @Setter private int skipPages = 0;

    public static final String USAGE_FILE_NUMBER = "Which file number to start on. Can be useful along with -k";
    public static final String OPT_FILE_NUMBER = "-n";
    public static final String LONGOPT_FILE_NUMBER= "--file-number";
    @Option(name=OPT_FILE_NUMBER, aliases=LONGOPT_FILE_NUMBER, usage=USAGE_FILE_NUMBER)
    @Getter @Setter private int fileNumber = 0;
}
