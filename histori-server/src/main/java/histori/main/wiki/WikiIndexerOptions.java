package histori.main.wiki;

import histori.wiki.linematcher.LineMatcher;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.io.ByteLimitedInputStream;
import org.kohsuke.args4j.Option;

import java.io.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

public class WikiIndexerOptions extends WikiBaseOptions {

    public static final String USAGE_INFILE = "Input wiki XML file. Default is stdin.";
    public static final String OPT_INFILE = "-i";
    public static final String LONGOPT_INFILE= "--infile";
    @Option(name=OPT_INFILE, aliases=LONGOPT_INFILE, usage=USAGE_INFILE)
    @Getter @Setter private File infile = null;

    public boolean hasInfile () { return infile != null; }

    public long infileSize () { return infile == null || !infile.exists() ? 0 : infile.length(); }

    public ByteLimitedInputStream getInputStream (long start, long end) throws IOException {
        if (hasInfile()) {
            final ByteLimitedInputStream in = new ByteLimitedInputStream(new FileInputStream(infile), end - start);
            if (start > 0 && in.skip(start) != start) die("getReader: skip failed ("+abs(infile)+")");
            return in;
        } else {
            if (start > 0 && System.in.skip(start) != start) die("getReader: skip failed (System.in)");
            return new ByteLimitedInputStream(System.in, Long.MAX_VALUE);
        }
    }

    public static final String USAGE_THREADS = "How many threads to use when indexing. Default is 1. For more than one, "+OPT_INFILE+"/"+LONGOPT_INFILE+" is required.";
    public static final String OPT_THREADS = "-t";
    public static final String LONGOPT_THREADS= "--threads";
    @Option(name=OPT_THREADS, aliases=LONGOPT_THREADS, usage=USAGE_THREADS)
    @Getter @Setter private int threads = 1;

    public static final String USAGE_SKIP = "How many bytes to skip from the beginning of the file. Do not use with more than one thread.";
    public static final String OPT_SKIP = "-s";
    public static final String LONGOPT_SKIP= "--skip";
    @Option(name=OPT_SKIP, aliases=LONGOPT_SKIP, usage=USAGE_SKIP)
    @Getter @Setter private long skip = 0;

    public boolean hasSkip () { return skip > 0; }

    public long getSkip (int i) {
        if (getThreads() == 1) return getSkip();
        return ((long) i) * (infileSize() / ((long) getThreads()));
    }

    public long getEnd (int i) {
        if (!hasInfile()) return Long.MAX_VALUE;
        if (getThreads() == 1) return infileSize();
        return ((long) i+1) * (infileSize() / ((long) getThreads()));
    }

    public static final String USAGE_FILTER = "Apply this filter to each article. Use a fully-qualified Java class name, must implement LineMatcher";
    public static final String OPT_FILTER = "-F";
    public static final String LONGOPT_FILTER= "--filter";
    @Option(name=OPT_FILTER, aliases=LONGOPT_FILTER, usage=USAGE_FILTER)
    @Getter @Setter private String filter;
    public boolean hasFilter () { return !empty(filter); }

    public static final String USAGE_FILTER_ARGS = "Arguments to the filter.";
    public static final String OPT_FILTER_ARGS = "-G";
    public static final String LONGOPT_FILTER_ARGS= "--filter-args";
    @Option(name=OPT_FILTER_ARGS, aliases=LONGOPT_FILTER_ARGS, usage=USAGE_FILTER_ARGS)
    @Getter @Setter private String filterArgs;
    public boolean hasFilterArgs () { return !empty(filterArgs); }

    public static final String USAGE_FILTER_LOG = "Article titles that matched the filter will be written to this file.";
    public static final String OPT_FILTER_LOG = "-L";
    public static final String LONGOPT_FILTER_LOG= "--filter-log";
    @Option(name=OPT_FILTER_LOG, aliases=LONGOPT_FILTER_LOG, usage=USAGE_FILTER_LOG)
    @Getter @Setter private File filterLog;
    public boolean hasFilterLog () { return filterLog != null; }

    @Getter(lazy=true) private final LineMatcher lineMatcher = initFilterObject();
    private LineMatcher initFilterObject() {
        if (empty(filter)) return null;
        final LineMatcher matcher = instantiate(filter);
        if (hasFilterArgs()) matcher.configure(filterArgs);
        return matcher;
    }

    public static final String USAGE_ARTICLE_LIST = "File containing a list of articles to read.";
    public static final String OPT_ARTICLE_LIST = "-A";
    public static final String LONGOPT_ARTICLE_LIST= "--articles";
    @Option(name=OPT_ARTICLE_LIST, aliases=LONGOPT_ARTICLE_LIST, usage=USAGE_ARTICLE_LIST)
    @Getter @Setter private File articleList;

}
