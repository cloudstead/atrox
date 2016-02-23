package histori.main.wiki;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import java.io.File;

public class ArticleNexusOptions extends WikiBaseOptions {

    public static final String USAGE_FILE = "Input: WikiArticle json file, or directory of json files. Default is to read a list of files or article names from stdin";
    public static final String OPT_FILE = "-i";
    public static final String LONGOPT_FILE= "--input-file";
    @Option(name=OPT_FILE, aliases=LONGOPT_FILE, usage=USAGE_FILE)
    @Getter @Setter private String input = null;

    public static final String USAGE_OVERWRITE = "Overwrite output files. Default is to preserve files.";
    public static final String OPT_OVERWRITE = "-O";
    public static final String LONGOPT_OVERWRITE= "--overwrite";
    @Option(name=OPT_OVERWRITE, aliases=LONGOPT_OVERWRITE, usage=USAGE_OVERWRITE)
    @Getter @Setter private boolean overwrite = false;

    public static final String USAGE_WIKI_DIR = "Base directory for wiki archive";
    public static final String OPT_WIKI_DIR = "-w";
    public static final String LONGOPT_WIKI_DIR= "--wiki-dir";
    @Option(name=OPT_WIKI_DIR, aliases=LONGOPT_WIKI_DIR, usage=USAGE_WIKI_DIR, required=true)
    @Getter @Setter private File wikiDir;

}
