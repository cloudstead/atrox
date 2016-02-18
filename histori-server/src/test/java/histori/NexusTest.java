package histori;

import histori.model.Account;
import histori.model.Nexus;
import histori.model.NexusTag;
import histori.model.support.AccountAuthResponse;
import histori.model.support.EntityCommentary;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.util.RestResponse;
import org.geojson.Point;
import org.junit.Before;
import org.junit.Test;

import static histori.ApiConstants.*;
import static histori.model.CanonicalEntity.canonicalize;
import static histori.model.support.TimePoint.TP_SEP;
import static org.cobbzilla.util.http.HttpStatusCodes.NOT_FOUND;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.string.StringUtil.urlEncode;
import static org.cobbzilla.wizardtest.RandomUtil.randomName;
import static org.junit.Assert.*;

public class NexusTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "Add world events to the map";

    private AccountAuthResponse authResponse;
    private Account account;

    @Before public void createAccount () throws Exception {
        authResponse = newAnonymousAccount();
        account = authResponse.getAccount();
    }

    @Test public void testNexusCrud () throws Exception {

        Nexus found;  // when we lookup by id
        Nexus result; // when we use the search api

        apiDocs.startRecording(DOC_TARGET, "Add a nexus and associated data");

        // define a range of one day of a random year in the distant past
        final int startYear = -1 * RandomUtils.nextInt(0, Integer.MAX_VALUE);
        final int startMonth = RandomUtils.nextInt(0, 12);
        final int startDay = RandomUtils.nextInt(0, 27);
        final String startDate = ""+startYear+ TP_SEP +startMonth + TP_SEP + startDay;
        final String endDate = ""+startYear+ TP_SEP +startMonth + TP_SEP + (startDay+1);

        apiDocs.addNote("Search the range ("+startDate+" to "+endDate+"), there should be nothing found");
        SearchResults<Nexus> searchResults = search(startDate, endDate);
        assertNull(searchResults.getTotalCount());
        assertEquals(0, searchResults.count());
        assertTrue(searchResults.getResults().isEmpty());

        final String nexusName = randomName();

        // Create a new Nexus
        final String headline = randomName();
        final Nexus nexus = new Nexus();
        nexus.setName(nexusName);
        nexus.setTimeRange(startDate, endDate);
        nexus.setGeo(new Point(0, 0));
        nexus.getCommentary().setHeadline(headline);
        nexus.addTag("War");
        nexus.addTag("USA", "world actor");
        nexus.getTag("usa").setCommentary(new EntityCommentary(headline+" for the usa"));
        nexus.addTag("http://example.com/citation.html", "citation");

        apiDocs.addNote("Define a new Nexus, and as a consequence, create some tags");
        Nexus createdNexus = fromJson(put("/nexus/"+urlEncode(nexusName), toJson(nexus)).json, Nexus.class);
        assertEquals(nexusName, createdNexus.getName());

        final String nexusPath = NEXUS_ENDPOINT + "/" + createdNexus.getUuid();

        apiDocs.addNote("Add tags");
        for (NexusTag tag : nexus.getTags()) {
            addTag(nexusPath, tag);
        }

        apiDocs.addNote("Lookup the Nexus we created by uuid");
        found = fromJson(get(nexusPath).json, Nexus.class);
        assertEquals(nexusName, found.getName());
        assertEquals(3, found.getTags().size());
        assertEquals(headline, found.getCommentary().getHeadline());

        apiDocs.addNote("Lookup the Nexus we created by name");
        found = fromJson(get(NEXUS_ENDPOINT+"/"+urlEncode(nexus.getName())).json, Nexus.class);
        assertEquals(nexusName, found.getName());
        assertEquals(3, found.getTags().size());

        apiDocs.addNote("Search for nexus in the same range, should see our new nexus");
        searchResults = search(startDate, endDate);
        assertEquals(1, searchResults.count());
        result = searchResults.getResult(0);
        assertEquals(nexusName, result.getName());
        assertEquals(3, result.getTags().size());

        apiDocs.addNote("Update our nexus with new name, this should create a new version");
        final String updatedName = nexusName + " -- update";
        nexus.setName(updatedName);
        nexus.getCommentary().setHeadline(headline + " -- update");
        final Nexus updatedNexus = fromJson(post(nexusPath, toJson(nexus)).json, Nexus.class);
        assertEquals(nexusName, updatedNexus.getName());

        apiDocs.addNote("Add another tag");
        nexus.addTag("Foobar");
        addTag(nexusPath, nexus.getTag("foobar"));

        apiDocs.addNote("Lookup the Nexus we updated by uuid, verify updated changes");
        found = fromJson(get(nexusPath).json, Nexus.class);
        assertEquals(nexusName, found.getName());
        assertEquals(4, found.getTags().size());
        assertTrue(found.hasTag("Foobar"));
        assertTrue(found.hasTag("foobar"));

        apiDocs.addNote("Search again, verify updated changes");
        searchResults = search(startDate, endDate);
        assertEquals(1, searchResults.count());
        result = searchResults.getResult(0);
        assertEquals(nexusName, result.getName());
        assertEquals(4, result.getTags().size());
        assertTrue(result.hasTag("foobar"));

        apiDocs.addNote("Update a tag");

        apiDocs.addNote("Lookup Nexus again, verify updated tag");

        apiDocs.addNote("Lookup previous versions, there should now be 2");

        apiDocs.addNote("Delete the nexus");
        delete(nexusPath);

        apiDocs.addNote("Lookup by id, should fail");
        assertEquals(NOT_FOUND, doGet(nexusPath).status);

        apiDocs.addNote("Search again, verify no results");
        searchResults = search(startDate, endDate);
        assertEquals(0, searchResults.count());

        apiDocs.addNote("Verify that tags are still present in the system");

        apiDocs.addNote("Verify that no NexusTags remain for the nexus");
    }

    public void addTag(String nexusPath, NexusTag tag) throws Exception {
        final String canonical = canonicalize(tag.getTagName());
        final String tagPath = nexusPath + ApiConstants.NEXUS_TAGS_ENDPOINT + "/" + urlEncode(canonical);
        final RestResponse response = put(tagPath, toJson(tag));
        final NexusTag createdTag = fromJson(response.json, NexusTag.class);
        assertEquals(createdTag.getTagName(), canonical);
    }

    public SearchResults<Nexus> search(String startDate, String endDate) throws Exception {
        return simpleSearch(SEARCH_ENDPOINT + EP_DATE + "/" + startDate + "/" + endDate, new Nexus().getSearchResultType());
    }

}
