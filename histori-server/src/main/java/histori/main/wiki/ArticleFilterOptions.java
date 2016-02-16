package histori.main.wiki;

import histori.wiki.TitleFilterType;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.regex.Pattern;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class ArticleFilterOptions extends BaseMainOptions {

    public static final String USAGE_DIR = "The directory containing JSON index files and article dump files (gzipped)";
    public static final String OPT_DIR = "-d";
    public static final String LONGOPT_DIR= "--dir";
    @Option(name=OPT_DIR, aliases=LONGOPT_DIR, usage=USAGE_DIR, required=true)
    @Getter @Setter private File dir;

    public static final String USAGE_OUTPUT_DIR = "Output directory. Default is current directory";
    public static final String OPT_OUTPUT_DIR = "-o";
    public static final String LONGOPT_OUTPUT_DIR= "--output-dir";
    @Option(name=OPT_OUTPUT_DIR, aliases=LONGOPT_OUTPUT_DIR, usage=USAGE_OUTPUT_DIR)
    @Getter @Setter private File outputDir = new File(System.getProperty("user.dir"));

    public static final String USAGE_FILTER_TYPE = "Title filter type. Default is prefix";
    public static final String OPT_FILTER_TYPE = "-t";
    public static final String LONGOPT_FILTER_TYPE= "--filter-type";
    @Option(name=OPT_FILTER_TYPE, aliases=LONGOPT_FILTER_TYPE, usage=USAGE_FILTER_TYPE)
    @Getter @Setter private TitleFilterType filterType = TitleFilterType.prefix;

    public static final String USAGE_FILTER_VALUE = "Include titles that match this value";
    public static final String OPT_FILTER_VALUE = "-f";
    public static final String LONGOPT_FILTER_VALUE= "--filter";
    @Option(name=OPT_FILTER_VALUE, aliases=LONGOPT_FILTER_VALUE, usage=USAGE_FILTER_VALUE, required=true)
    @Getter @Setter private String filter;

    @Getter(lazy=true) private final Pattern filterPattern = Pattern.compile(filter);

    public boolean isFilterMatch(String title) {
        switch (filterType) {
            case prefix:   return title.toLowerCase().startsWith(filter) && !title.contains("(disambiguation)");
            case suffix:   return title.toLowerCase().endsWith(filter) && !title.contains("(disambiguation)");
            case contains: return title.toLowerCase().contains(filter) && !title.contains("(disambiguation)");
            case regex:    return getFilterPattern().matcher(title).matches();
            default:       return die("isFilterMatch: invalid filter type: "+filterType);
        }
    }
}
