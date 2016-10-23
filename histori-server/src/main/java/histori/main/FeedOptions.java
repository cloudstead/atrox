package histori.main;

import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import java.io.File;

import static org.cobbzilla.util.io.FileUtil.toStringOrDie;

public class FeedOptions extends HistoriApiOptions {

    public static final String USAGE_FEED_JSON = "Feed JSON";
    public static final String OPT_FEED_JSON = "-f";
    public static final String LONGOPT_FEED_JSON = "--feed";
    @Option(name=OPT_FEED_JSON, aliases=LONGOPT_FEED_JSON, usage=USAGE_FEED_JSON, required=true)
    @Setter private String feedJson;

    public static final String USAGE_PREVIEW = "Save feed and view results";
    public static final String OPT_PREVIEW = "-w";
    public static final String LONGOPT_PREVIEW = "--preview";
    @Option(name=OPT_PREVIEW, aliases=LONGOPT_PREVIEW, usage=USAGE_PREVIEW)
    @Getter @Setter private boolean preview = false;

    public static final String USAGE_SAVE = "Save feed; view and save results. Implies preview.";
    public static final String OPT_SAVE = "-W";
    public static final String LONGOPT_SAVE = "--save";
    @Option(name=OPT_SAVE, aliases=LONGOPT_SAVE, usage=USAGE_SAVE)
    @Getter @Setter private boolean save = false;

    public String getFeedJson() {
        final File f = new File(feedJson);
        return f.exists() ? toStringOrDie(f) : feedJson;
    }

}
