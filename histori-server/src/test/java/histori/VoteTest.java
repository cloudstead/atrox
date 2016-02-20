package histori;

import histori.model.Nexus;
import histori.model.Vote;
import histori.model.support.AccountAuthResponse;
import org.junit.Before;
import org.junit.Test;

import static histori.ApiConstants.*;
import static org.cobbzilla.util.http.HttpStatusCodes.NOT_FOUND;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.junit.Assert.assertEquals;

public class VoteTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "Up-vote, down-vote, and view votes";

    private Nexus nexus;
    private AccountAuthResponse authResponse;

    @Before public void populate () throws Exception {
        authResponse = newAnonymousAccount();
        nexus = createNexus(dummyNexus());
        // todo: create some tags that could also be voted on
    }

    @Test public void testVotingCrud () throws Exception {

        apiDocs.startRecording(DOC_TARGET, "Exercise basic voting features");
        Vote vote;
        final String voteUri = voteUri(nexus);

        apiDocs.addNote("View vote on nexus, should be 404 Not Found");
        assertEquals(NOT_FOUND, doGet(VOTES_ENDPOINT+"/"+nexus.getUuid()).status);

        apiDocs.addNote("Upvote");
        vote = fromJson(post(upvoteUri(nexus), null).json, Vote.class);
        assertEquals(1, vote.getVote());

        apiDocs.addNote("View vote on nexus, should be upvote");
        vote = fromJson(get(voteUri).json, Vote.class);
        assertEquals(1, vote.getVote());

        apiDocs.addNote("Downvote");
        vote = fromJson(post(downvoteUri(nexus), null).json, Vote.class);
        assertEquals(-1, vote.getVote());

        apiDocs.addNote("View vote on nexus, should be downvote");
        vote = fromJson(get(voteUri).json, Vote.class);
        assertEquals(-1, vote.getVote());

        apiDocs.addNote("Delete vote");
        delete(voteUri);

        apiDocs.addNote("View vote on nexus, should be 404 Not Found");
        assertEquals(NOT_FOUND, doGet(voteUri).status);

        apiDocs.addNote("Upvote again");
        vote = fromJson(post(upvoteUri(nexus), null).json, Vote.class);
        assertEquals(1, vote.getVote());

        apiDocs.addNote("View vote on nexus, should be upvote");
        vote = fromJson(get(voteUri).json, Vote.class);
        assertEquals(1, vote.getVote());

        // todo: check voting history? ensure it looks correct
    }

}
