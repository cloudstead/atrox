package histori;

import histori.model.Account;
import histori.model.AccountAuthResponse;
import histori.model.canonical.WorldEvent;
import histori.model.history.WorldActorHistory;
import histori.model.history.WorldEventHistory;
import histori.model.support.GeoPolygon;
import histori.model.support.TimePoint;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.wizard.dao.SearchResults;
import org.junit.Before;
import org.junit.Test;

import static histori.ApiConstants.EP_BY_DATE;
import static histori.ApiConstants.historyEndpoint;
import static histori.model.support.TimePoint.TP_SEP;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.wizardtest.RandomUtil.randomName;
import static org.junit.Assert.*;

public class WorldEventsTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "Add world events to the map";

    private AccountAuthResponse authResponse;
    private Account account;

    @Before public void createAccount () throws Exception {
        authResponse = newAnonymousAccount();
        account = authResponse.getAccount();
    }

    @Test public void testAddWorldEvent () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "Add a world event and associated data");

        // define a range of one day of a random year in the distant past
        final int startYear = -1 * RandomUtils.nextInt(0, Integer.MAX_VALUE);
        final int startMonth = RandomUtils.nextInt(0, 12);
        final int startDay = RandomUtils.nextInt(0, 27);
        final String startDate = ""+startYear+ TP_SEP +startMonth + TP_SEP + startDay;
        final String endDate = ""+startYear+ TP_SEP +startMonth + TP_SEP + (startDay+1);

        apiDocs.addNote("Search for event ideas in the range ("+startDate+" to "+endDate+"), there should be none");
        SearchResults<WorldEventHistory> foundEvents = findWorldEvents(startDate, endDate);
        assertNull(foundEvents.getTotalCount());
        assertEquals(0, foundEvents.count());
        assertTrue(foundEvents.getResults().isEmpty());

        // Create a new WorldEventHistory
        final String eventName = randomName();
        final String headline = randomName();
        final WorldEventHistory history = new WorldEventHistory();
        history.setWorldEvent(eventName);
        history.setStartPoint(new TimePoint(startDate));
        history.setEndPoint(new TimePoint(endDate));
        history.setPolygon(new GeoPolygon("0,0", "0.1,0.1"));
        history.getCommentary().setHeadline(headline);
        history.addIdeas("east asia");
        history.addCitations("http://example.com/citation.html");

        final WorldActorHistory germany = new WorldActorHistory();
        germany.setWorldActor("Germany");
        germany.addIdea("nazism");
        germany.addIdea("axis");
        germany.addIdea("hitler");
        germany.addCitation("http://example.com/something.html");

        final WorldActorHistory italy = new WorldActorHistory();
        germany.setWorldActor("Italy");
        germany.addIdea("fascism");
        germany.addIdea("axis");
        germany.addIdea("hitler");
        germany.addCitation("http://example.com/something.html");

        history.addActor(germany);
        history.addActor(italy);
//        history.addImpact();

        apiDocs.addNote("Define a new WorldEventHistory, and as a consequence, create the canonical WorldEvent");
        WorldEventHistory createdWETag = fromJson(post(historyEndpoint(WorldEventHistory.class), toJson(history)).json, WorldEventHistory.class);
        assertNotNull(createdWETag);
        assertNotEquals(eventName, createdWETag.getWorldEvent());// should now be a uuid

        apiDocs.addNote("Lookup the WorldEventHistory we created by uuid");
        WorldEventHistory foundTag = fromJson(get(historyEndpoint(WorldEventHistory.class)+"/"+createdWETag.getUuid()).json, WorldEventHistory.class);
        WorldEvent worldEvent = (WorldEvent) foundTag.getCanonical();
        assertNotNull(worldEvent);
        assertEquals(eventName, worldEvent.getName());

        apiDocs.addNote("Search for world events in the same range, should see the new canonical event with our single tag");
        foundEvents = findWorldEvents(startDate, endDate);
        assertEquals(1, foundEvents.count());

        apiDocs.addNote("Update our tag, this should create a new version");

        apiDocs.addNote("Search for event ideas, we should see our updated tag");
    }

    public SearchResults<WorldEventHistory> findWorldEvents(String startDate, String endDate) throws Exception {
        return simpleSearch(historyEndpoint(WorldEvent.class)
                + "/" + EP_BY_DATE
                + "/" + startDate
                + "/" + endDate, new WorldEventHistory().getSearchResultType());
    }

}
