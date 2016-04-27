package histori.main;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import java.io.File;

public class NexusImportOptions extends HistoriApiOptions {

    public static final String USAGE_FILE = "Input: Nexus json file, or directory of nexus json files";
    public static final String OPT_FILE = "-i";
    public static final String LONGOPT_FILE= "--input-file";
    @Option(name=OPT_FILE, aliases=LONGOPT_FILE, usage=USAGE_FILE, required=true)
    @Getter @Setter private File file = null;

    public static final String USAGE_FORCE = "When bulk loading, force-cancels any previous bulk loading job";
    public static final String OPT_FORCE = "-F";
    public static final String LONGOPT_FORCE= "--force";
    @Option(name=OPT_FORCE, aliases=LONGOPT_FORCE, usage=USAGE_FORCE)
    @Getter @Setter private boolean force = false;

    public static final String USAGE_AUTHORITATIVE = "Sets the 'authoritative' flag on imported Nexuses. Must be admin user.";
    public static final String OPT_AUTHORITATIVE = "-A";
    public static final String LONGOPT_AUTHORITATIVE= "--authoritative";
    @Option(name=OPT_AUTHORITATIVE, aliases=LONGOPT_AUTHORITATIVE, usage=USAGE_AUTHORITATIVE)
    @Getter @Setter private boolean authoritative = false;

}
