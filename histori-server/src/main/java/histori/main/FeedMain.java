package histori.main;

import histori.ApiConstants;
import histori.model.Feed;
import org.cobbzilla.wizard.client.ApiClientBase;

import static org.cobbzilla.util.json.JsonUtil.json;

public class FeedMain extends HistoriApiMain<FeedOptions> {

    public static void main (String[] args) { main(FeedMain.class, args); }

    @Override protected void run() throws Exception {
        final ApiClientBase api = getApiClient();
        final FeedOptions options = getOptions();
        final Feed feed = options.getFeed();
        out(api.post(ApiConstants.FEEDS_ENDPOINT, json(feed)));
    }
}
