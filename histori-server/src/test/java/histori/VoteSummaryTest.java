package histori;

import histori.dao.cache.VoteSummaryDAO;
import histori.model.Nexus;
import histori.model.cache.VoteSummary;
import histori.model.support.AccountAuthResponse;
import histori.model.support.TimeRange;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.util.daemon.ZillaRuntime;
import org.cobbzilla.util.system.Sleep;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static histori.ApiConstants.*;
import static org.cobbzilla.util.http.HttpStatusCodes.NOT_FOUND;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class VoteSummaryTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "View vote summaries";

    public static final int MAX_VOTERS = 20;
    public static final int MAX_NEXUSES = 3;

    private TimeRange range;
    private List<AccountAuthResponse> accounts = new ArrayList<>();
    private List<Nexus> nexuses = new ArrayList<>();
    private Map<String, Long> expectedTallies = new HashMap<>();

    @Before public void populate () throws Exception {

        for (int i=0; i<MAX_VOTERS; i++) accounts.add(newAnonymousAccount());

        range = randomTimeRange();
        nexuses.clear();
        for (int i = 0; i<MAX_NEXUSES; i++) nexuses.add(createNexus(dummyNexus(range)));

        for (Nexus nexus : nexuses) {
            // each account casts a random vote, usually an upvote
            long tally = 0;
            for (int i=0; i<MAX_VOTERS; i++) {
                pushToken(accounts.get(i).getSessionId());
                boolean up = RandomUtils.nextInt(0, 10) < 8; // 80% upvotes
                if (up) {
                    post(upvoteUri(nexus), null);
                    tally++;

                } else {
                    post(downvoteUri(nexus), null);
                    tally--;
                }
                popToken();
            }
            expectedTallies.put(nexus.getUuid(), tally);
        }
    }

    private static final long SUMMARY_CALC_TIMEOUT = TimeUnit.SECONDS.toMillis(60);

    @Test public void testVotes () throws Exception {

        apiDocs.startRecording(DOC_TARGET, "View vote summaries for a variety of things");

        apiDocs.addNote("Get vote summary for a single nexus, should return null but kick off a process in the background");
        final Nexus nexus = nexuses.get(0);
        final String uuid = nexus.getUuid();
        final String voteUri = voteUri(nexus);

        assertEquals(NOT_FOUND, doGet(voteUri+EP_SUMMARY).status);

        // secretly poll VoteSummaryDAO to see when it finishes
        // timeout after 10 seconds
        final VoteSummaryDAO voteSummaryDAO = getBean(VoteSummaryDAO.class);
        long start = ZillaRuntime.now();
        while (voteSummaryDAO.isRunning(uuid) && ZillaRuntime.now() - start < SUMMARY_CALC_TIMEOUT) {
            Sleep.sleep(200);
        }
        if (ZillaRuntime.now() > start + SUMMARY_CALC_TIMEOUT) fail("timeout awaiting vote summary calculation for: "+uuid);

        apiDocs.addNote("Get vote summary for a single nexus, should return valid summary from redis");
        final VoteSummary summary = fromJson(get(voteUri+EP_SUMMARY).json, VoteSummary.class);
        assertEquals(expectedTallies.get(uuid), (Long) summary.getTally());
    }

}
