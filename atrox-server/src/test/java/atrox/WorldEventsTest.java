package atrox;

import atrox.model.*;
import atrox.model.support.GeoPolygon;
import atrox.model.support.TimePoint;
import atrox.model.tags.EventEffectTag;
import atrox.model.view.WorldEventView;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.wizard.dao.SearchResults;
import org.junit.Before;
import org.junit.Test;

import static atrox.ApiConstants.EP_EVENTS_BY_DATE;
import static atrox.ApiConstants.NAMED_ENTITIES_ENDPOINT;
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

        apiDocs.addNote("Search for events in the range ("+startDate+" to "+endDate+"), there should be none");
        SearchResults<WorldEventView> foundEvents = findWorldEvents(startDate, endDate);
        assertNull(foundEvents.getTotalCount());
        assertEquals(0, foundEvents.count());
        assertTrue(foundEvents.getResults().isEmpty());

        final String eventName = randomName();
        final WorldEvent event = new WorldEvent();
        event.setName(eventName);
        event.setStartPoint(new TimePoint(startDate));
        event.setEndPoint(new TimePoint(endDate));
        event.setPolygon(new GeoPolygon("0,0|0.1,0.1"));

        // Define a WorldActor
        final String actorName = randomName();
        final WorldActor actor = new WorldActor(actorName);

        // Define an effect
        final String effectType = randomName();
        final EventEffectTag effect = new EventEffectTag().setEffectType(effectType);
        long randomEstimate = RandomUtils.nextLong(10_000, 100_000);
        effect.setLowEstimate(randomEstimate/2);
        effect.setMidEstimate(randomEstimate);
        effect.setHighEstimate(randomEstimate*2);

        // Build the view
        final WorldEventView view = new WorldEventView(event).addActor(actor).addEffect(effect);

        apiDocs.addNote("Define a new world event");
        final WorldEventView created = fromJson(post(NAMED_ENTITIES_ENDPOINT +"/event", toJson(view)).json, WorldEventView.class);
        assertNotNull(created);
        assertEquals(eventName, created.getWorldEvent().getName());
        assertEquals(1, created.getActors().size());
        assertEquals(actorName, created.getActors().get(0).getName());
        assertEquals(1, created.getEffects().size());
        assertEquals(effectType, created.getEffects().get(0).getEffectType());

        apiDocs.addNote("Search for events in the range ("+startDate+" to "+endDate+"), we should fine the event we just created");
        foundEvents = findWorldEvents(startDate, endDate);
        assertEquals(1, foundEvents.total());
        assertEquals(1, foundEvents.count());
        assertFalse(foundEvents.getResults().isEmpty());

        final WorldEventView foundEvent = foundEvents.getResult(0);
        assertEquals(eventName, foundEvent.getWorldEvent().getName());
        assertEquals(1, foundEvent.getActors().size());
        assertEquals(actorName, foundEvent.getActors().get(0).getName());
        assertEquals(1, foundEvent.getEffects().size());
        assertEquals(effectType, foundEvent.getEffects().get(0).getEffectType());
    }

    public SearchResults<WorldEventView> findWorldEvents(String startDate, String endDate) throws Exception {
        return simpleSearch(NAMED_ENTITIES_ENDPOINT +EP_EVENTS_BY_DATE+"/"+startDate+"/"+endDate, WorldEventView.searchResultType);
    }

}
