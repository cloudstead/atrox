package histori.main.wiki;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import java.io.File;

public class WikiIndexerOptions extends WikiBaseOptions {

    public static final String USAGE_SKIP = "How many pages to skip at the start";
    public static final String OPT_SKIP = "-k";
    public static final String LONGOPT_SKIP= "--skip-pages";
    @Option(name=OPT_SKIP, aliases=LONGOPT_SKIP, usage=USAGE_SKIP)
    @Getter @Setter private int skipPages = 0;

    public static final String USAGE_SKIP_LINES = "How many lines to skip at the start";
    public static final String OPT_SKIP_LINES = "-L";
    public static final String LONGOPT_SKIP_LINES= "--skip-lines";
    @Option(name=OPT_SKIP_LINES, aliases=LONGOPT_SKIP_LINES, usage=USAGE_SKIP_LINES)
    @Getter @Setter private int skipLines = 0;

    public static final String USAGE_ARTICLE_LIST = "File containing a list of articles to read.";
    public static final String OPT_ARTICLE_LIST = "-A";
    public static final String LONGOPT_ARTICLE_LIST= "--articles";
    @Option(name=OPT_ARTICLE_LIST, aliases=LONGOPT_ARTICLE_LIST, usage=USAGE_ARTICLE_LIST)
    @Getter @Setter private File articleList;

}
