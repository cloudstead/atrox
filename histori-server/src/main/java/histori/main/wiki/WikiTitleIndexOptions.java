package histori.main.wiki;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class WikiTitleIndexOptions extends BaseMainOptions {

    public static final String USAGE_FILE = "File to read. Default is stdin.";
    public static final String OPT_FILE = "-f";
    public static final String LONGOPT_FILE= "--file";
    @Option(name=OPT_FILE, aliases=LONGOPT_FILE, usage=USAGE_FILE)
    @Getter @Setter private File file = null;
    public boolean hasFile () { return file != null; }

    public InputStream getStream() throws Exception {
        return hasFile() ? new FileInputStream(file) : System.in;
    }

    public static final String USAGE_SORT = "Sort results alphabetically (ignoring case)";
    public static final String OPT_SORT = "-S";
    public static final String LONGOPT_SORT= "--sort";
    @Option(name=OPT_SORT, aliases=LONGOPT_SORT, usage=USAGE_SORT)
    @Getter @Setter private boolean sort = false;

    public static final String USAGE_SIZE = "Size hint for sorting, helps to set up the proper data storage";
    public static final String OPT_SIZE = "-z";
    public static final String LONGOPT_SIZE= "--size";
    @Option(name=OPT_SIZE, aliases=LONGOPT_SIZE, usage=USAGE_SIZE)
    @Getter @Setter private int size = 0;

    public int getSortSize () { return sort ? (size == 0 ? 1_000_000 : size) : 0; }

}
