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

}
