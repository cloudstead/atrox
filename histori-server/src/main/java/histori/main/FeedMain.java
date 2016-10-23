package histori.main;

import histori.model.Feed;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;

import static histori.ApiConstants.FEEDS_ENDPOINT;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.string.StringUtil.urlEncode;

public class FeedMain extends HistoriApiMain<FeedOptions> {

    public static void main (String[] args) { main(FeedMain.class, args); }

    @Override protected void run() throws Exception {
        final ApiClientBase api = getApiClient();
        final FeedOptions options = getOptions();
        final RestResponse response = api.post(FEEDS_ENDPOINT, options.getFeedJson());
        final Feed created = json(response.json, Feed.class);
        out(json(created));
        out(api.get(getItemsUri(options, created)));
    }

    private String getItemsUri(FeedOptions options, Feed feed) {
        return FEEDS_ENDPOINT
                +"/"+urlEncode(feed.getName())
                +"/items"
                +(options.isSave() ? "?save=true" : options.isPreview() ? "?preview=true" : "");
    }

}
