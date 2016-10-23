package histori.main;

import histori.model.Feed;
import org.cobbzilla.wizard.client.ApiClientBase;

import static histori.ApiConstants.FEEDS_ENDPOINT;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.string.StringUtil.urlEncode;

public class FeedMain extends HistoriApiMain<FeedOptions> {

    public static void main (String[] args) { main(FeedMain.class, args); }

    @Override protected void run() throws Exception {
        final ApiClientBase api = getApiClient();
        final FeedOptions options = getOptions();
        final Feed feed = api.post(FEEDS_ENDPOINT, options.getFeedJson(), Feed.class);
        out(json(feed));
        out(api.get(getFeedUri(options, feed)));
    }

    private String getFeedUri(FeedOptions options, Feed feed) {
        return FEEDS_ENDPOINT
                +"/"+urlEncode(feed.getName())
                +"/items?"
                +(options.isSave() ? "?save=true" : options.isPreview() ? "?preview=true" : "");
    }

}
