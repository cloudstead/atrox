package histori.main.wiki;

import histori.wiki.linematcher.LineMatcher;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import java.io.File;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

public class WikiIndexerOptions extends WikiBaseOptions {

    public static final String USAGE_SKIP = "How many pages to skip at the start. Default is zero";
    public static final String OPT_SKIP = "-k";
    public static final String LONGOPT_SKIP= "--skip-pages";
    @Option(name=OPT_SKIP, aliases=LONGOPT_SKIP, usage=USAGE_SKIP)
    @Getter @Setter private int skipPages = 0;

    public static final String USAGE_SKIP_LINES = "How many lines to skip at the start. Default is zero.";
    public static final String OPT_SKIP_LINES = "-s";
    public static final String LONGOPT_SKIP_LINES= "--skip-lines";
    @Option(name=OPT_SKIP_LINES, aliases=LONGOPT_SKIP_LINES, usage=USAGE_SKIP_LINES)
    @Getter @Setter private int skipLines = 0;

    public static final String USAGE_STOP_LINES = "Stop after reading this many lines. Note that a few more lines may be read in order to complete an open article. Default is not to stop until EOF";
    public static final String OPT_STOP_LINES = "-S";
    public static final String LONGOPT_STOP_LINES= "--stop-lines";
    @Option(name=OPT_STOP_LINES, aliases=LONGOPT_STOP_LINES, usage=USAGE_STOP_LINES)
    @Getter @Setter private int stopLines = -1;

    public static final String USAGE_FILTER = "Apply this filter to each article. Use a fully-qualified Java class name, must implement LineMatcher";
    public static final String OPT_FILTER = "-F";
    public static final String LONGOPT_FILTER= "--filter";
    @Option(name=OPT_FILTER, aliases=LONGOPT_FILTER, usage=USAGE_FILTER)
    @Getter @Setter private String filter;
    public boolean hasFilter () { return !empty(filter); }

    public static final String USAGE_FILTER_LOG = "Article titles that matched the filter will be written to this file.";
    public static final String OPT_FILTER_LOG = "-L";
    public static final String LONGOPT_FILTER_LOG= "--filter-log";
    @Option(name=OPT_FILTER_LOG, aliases=LONGOPT_FILTER_LOG, usage=USAGE_FILTER_LOG)
    @Getter @Setter private File filterLog;
    public boolean hasFilterLog () { return filterLog != null; }

    @Getter(lazy=true) private final LineMatcher lineMatcher = initFilterObject();
    private LineMatcher initFilterObject() { return empty(filter) ? null : (LineMatcher) instantiate(filter); }

    public static final String USAGE_ARTICLE_LIST = "File containing a list of articles to read.";
    public static final String OPT_ARTICLE_LIST = "-A";
    public static final String LONGOPT_ARTICLE_LIST= "--articles";
    @Option(name=OPT_ARTICLE_LIST, aliases=LONGOPT_ARTICLE_LIST, usage=USAGE_ARTICLE_LIST)
    @Getter @Setter private File articleList;

}
