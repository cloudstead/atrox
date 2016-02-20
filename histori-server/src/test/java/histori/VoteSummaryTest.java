package histori;

import histori.model.Nexus;
import histori.model.support.TimeRange;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static histori.ApiConstants.EP_UPVOTE;
import static histori.ApiConstants.VOTES_ENDPOINT;

public class VoteSummaryTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "View vote summaries";

    private static final int MAX_VOTERS = 10;
    private TimeRange range;
    private List<String> accountTokens = new ArrayList<>();
    private List<Nexus> nexuses = new ArrayList<>();

    @Before public void populate () throws Exception {

        range = randomTimeRange();
        nexuses.clear();
        for (int i=0; i<100; i++) {
            nexuses.add(createNexus(dummyNexus(range)));
        }

        for (Nexus nexus : nexuses) {
            // cast some random votes
            int randomUp = RandomUtils.nextInt(0, MAX_VOTERS);
            for (int i=0; i<randomUp; i++) {
                pushToken(accountTokens.get(i));
                post(VOTES_ENDPOINT+"/"+nexus.getUuid()+"/"+ EP_UPVOTE, null);
                popToken();
            }
            int randomDown = RandomUtils.nextInt(0, MAX_VOTERS/2);

        }
    }

    @Test public void testVotes () throws Exception {

        apiDocs.startRecording(DOC_TARGET, "View vote summaries for a variety of things");

        // API get vote summary -- should return null but kick off a process in the background

        // secretly poll VoteSummaryDAO to see when it finishes
        // timeout after 10 seconds

        // API get vote summary -- now should succeed and match
    }

}
