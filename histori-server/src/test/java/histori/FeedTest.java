package histori;

import org.junit.Test;

import static org.cobbzilla.util.io.StreamUtil.stream2string;

public class FeedTest extends ApiRunnerTest {

    private static final String DOC_TARGET = "Add a data feed, verify nexuses are added";

    @Test public void testGdacsEarthquakeFeed () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "register GDAC earthquake filter, verify nexuses are added");
        getApiRunner().run(stream2string("tests/add_gdac_feed.json"));
    }

}
