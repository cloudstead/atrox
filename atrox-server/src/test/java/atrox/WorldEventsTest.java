package atrox;

import atrox.model.Account;
import atrox.model.AccountAuthResponse;
import atrox.model.WorldEvent;
import atrox.model.support.GeoPolygon;
import atrox.model.support.TimePoint;
import atrox.model.tags.WorldEventTag;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.wizard.dao.SearchResults;
import org.junit.Before;
import org.junit.Test;

import static atrox.ApiConstants.EP_BY_DATE;
import static atrox.ApiConstants.entityEndpoint;
import static atrox.model.support.TimePoint.TP_SEP;
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

        apiDocs.addNote("Search for event tags in the range ("+startDate+" to "+endDate+"), there should be none");
        SearchResults<WorldEventTag> foundEvents = findWorldEventTags(startDate, endDate);
        assertNull(foundEvents.getTotalCount());
        assertEquals(0, foundEvents.count());
        assertTrue(foundEvents.getResults().isEmpty());

        // Create a new WorldEventTag
        final String eventName = randomName();
        final String headline = randomName();
        final WorldEventTag worldEventTag = new WorldEventTag();
        worldEventTag.setWorldEvent(eventName);
        worldEventTag.setStartPoint(new TimePoint(startDate));
        worldEventTag.setEndPoint(new TimePoint(endDate));
        worldEventTag.setPolygon(new GeoPolygon("0,0", "0.1,0.1"));
        worldEventTag.getCommentary().setHeadline(headline);

        apiDocs.addNote("Define a new WorldEventTag, and as a consequence, create the canonical WorldEvent");
        WorldEventTag createdWETag = fromJson(post(entityEndpoint(WorldEventTag.class), toJson(worldEventTag)).json, WorldEventTag.class);
        assertNotNull(createdWETag);
        assertNotEquals(eventName, createdWETag.getWorldEvent());// should now be a uuid

        apiDocs.addNote("Lookup the WorldEventTag we created by uuid");
        WorldEventTag foundTag = fromJson(get(entityEndpoint(WorldEventTag.class)+"/"+createdWETag.getUuid()).json, WorldEventTag.class);
        WorldEvent worldEvent = (WorldEvent) foundTag.getAssociation(WorldEvent.class);
        assertNotNull(worldEvent);
        assertEquals(eventName, worldEvent.getName());

        apiDocs.addNote("Search for world events in the same range, should see the new canonical event with our single tag");

        apiDocs.addNote("Update our tag, this should create a new version");

        apiDocs.addNote("Search for event tags, we should see our updated tag");
    }

    public SearchResults<WorldEventTag> findWorldEventTags(String startDate, String endDate) throws Exception {
        return simpleSearch(entityEndpoint(WorldEventTag.class)
                + "/" + EP_BY_DATE
                + "/" + startDate
                + "/" + endDate, new WorldEventTag().getSearchResultType());
    }

}
