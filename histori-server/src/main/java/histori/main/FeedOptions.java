package histori.main;

import histori.model.Feed;
import lombok.Setter;
import org.kohsuke.args4j.Option;

import java.io.File;

import static org.cobbzilla.util.io.FileUtil.toStringOrDie;
import static org.cobbzilla.util.json.JsonUtil.json;

public class FeedOptions extends HistoriApiOptions {

    public static final String USAGE_FEED_JSON = "Feed JSON";
    public static final String OPT_FEED_JSON = "-f";
    public static final String LONGOPT_FEED_JSON = "--feed";
    @Option(name=OPT_FEED_JSON, aliases=LONGOPT_FEED_JSON, usage=USAGE_FEED_JSON, required=true)
    @Setter private String feedJson;

    private String getFeedJson() {
        final File f = new File(feedJson);
        return f.exists() ? toStringOrDie(f) : feedJson;
    }

    public Feed getFeed() { return json(getFeedJson(), Feed.class); }

}
