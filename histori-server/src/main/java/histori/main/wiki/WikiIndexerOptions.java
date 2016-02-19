package histori.main.wiki;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import java.io.File;

public class WikiIndexerOptions extends HistoriS3Options {

    public static final String USAGE_OUTPUT_DIR = "Output directory for split files. Default is current directory";
    public static final String OPT_OUTPUT_DIR = "-o";
    public static final String LONGOPT_OUTPUT_DIR= "--output-dir";
    @Option(name=OPT_OUTPUT_DIR, aliases=LONGOPT_OUTPUT_DIR, usage=USAGE_OUTPUT_DIR)
    @Getter @Setter private File outputDir = new File(System.getProperty("user.dir"));

    public static final String USAGE_SKIP = "How many pages to skip at the start";
    public static final String OPT_SKIP = "-k";
    public static final String LONGOPT_SKIP= "--skip-pages";
    @Option(name=OPT_SKIP, aliases=LONGOPT_SKIP, usage=USAGE_SKIP)
    @Getter @Setter private int skipPages = 0;

}
