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
    public static final String LONGOPT_FILE= "--help";
    @Option(name=OPT_FILE, aliases=LONGOPT_FILE, usage=USAGE_FILE)
    @Getter @Setter private File file = null;
    public boolean hasFile () { return file != null; }

    public InputStream getStream() throws Exception {
        return hasFile() ? new FileInputStream(file) : System.in;
    }

}
