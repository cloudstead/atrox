package histori;

import histori.archive.NexusArchive;
import histori.archive.NexusTagArchive;
import histori.dao.NexusSummaryDAO;
import histori.model.*;
import histori.model.support.*;
import org.cobbzilla.wizard.dao.SearchResults;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static histori.ApiConstants.*;
import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.http.HttpStatusCodes.NOT_FOUND;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.string.StringUtil.urlEncode;
import static org.cobbzilla.util.system.Sleep.sleep;
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
        NexusSummary result; // when we use the search api

        apiDocs.startRecording(DOC_TARGET, "Add a nexus and associated data");

        // define a range of one day of a random year in the distant past
        final TimeRange range = randomTimeRange();
        final String startDate = range.getStartPoint().toString();
        final String endDate = range.getEndPoint().toString();

        apiDocs.addNote("Search the range ("+range.getStartPoint()+" to "+range.getEndPoint()+"), there should be nothing found");
        SearchResults<NexusSummary> searchResults = search(startDate, endDate);
        assertNull(searchResults.getTotalCount());
        assertEquals(0, searchResults.count());
        assertTrue(searchResults.getResults().isEmpty());

        final String nexusName = randomName();

        final String tag1 = "War";
        final String tag2 = "USA";
        final String tag3 = "http://example.com/citation.html";

        // Create a new Nexus
        final String headline = randomName();
        final Nexus nexus = newNexus(startDate, endDate, nexusName, headline);
        nexus.addTag(tag1);
        nexus.addTag(tag2, "world actor");
        nexus.getFirstTag("usa").setCommentary(new EntityCommentary(headline+" for the usa"));
        nexus.addTag(tag3, "citation");

        apiDocs.addNote("Define a new Nexus");
        Nexus createdNexus = createNexus(nexusName, nexus);

        final String nexusPath = NEXUS_ENDPOINT + "/" + createdNexus.getUuid();

        apiDocs.addNote("Add tags");
        for (NexusTag tag : nexus.getTags()) {
            addTag(nexusPath, tag);
        }

        apiDocs.addNote("Lookup the Nexus we created by uuid");
        found = fromJson(get(nexusPath).json, Nexus.class);
        assertEquals(nexusName, found.getName());
        assertEquals(3, found.getTags().size());
        assertEquals(headline, found.initCommentary().getHeadline());

        apiDocs.addNote("Lookup the Nexus we created by name");
        found = fromJson(get(NEXUS_ENDPOINT+"/"+urlEncode(nexus.getName())).json, Nexus.class);
        assertEquals(nexusName, found.getName());
        assertEquals(3, found.getTags().size());

        apiDocs.addNote("Search for nexus in the same range, should see our new nexus, but probably without tags");
        searchResults = search(startDate, endDate);
        assertEquals(1, searchResults.count());
        result = searchResults.getResult(0);
        assertEquals(nexusName, result.getPrimary().getName());

        // wait for tags to exist (a background job will do this)
        final NexusSummaryDAO summaryDAO = getBean(NexusSummaryDAO.class);
        while (summaryDAO.get(summaryDAO.cacheKey(nexus, account, EntityVisibility.everyone)) == null) sleep(50);

        apiDocs.addNote("Search for nexus in the same range, should see our new nexus, now with tags");
        searchResults = search(startDate, endDate);
        assertEquals(1, searchResults.count());
        result = searchResults.getResult(0);
        assertEquals(nexusName, result.getPrimary().getName());
        assertEquals(3, result.getPrimary().getTags().size());

        apiDocs.addNote("Update our nexus with new name, this should create a new version");
        final String updatedName = nexusName + " -- update";
        nexus.setName(updatedName);
        nexus.initCommentary().setHeadline(headline + " -- update");
        final Nexus updatedNexus = fromJson(post(nexusPath, toJson(nexus)).json, Nexus.class);
        assertEquals(nexusName, updatedNexus.getName());

        apiDocs.addNote("Add another tag");
        String tag4 = "Foobar";
        nexus.addTag(tag4);
        addTag(nexusPath, nexus.getFirstTag("foobar"));

        apiDocs.addNote("Lookup the Nexus we updated by uuid, verify updated changes");
        found = fromJson(get(nexusPath).json, Nexus.class);
        assertEquals(nexusName, found.getName());
        assertEquals(4, found.getTags().size());
        assertTrue(found.hasTag(tag4));
        assertTrue(found.hasTag(tag4.toLowerCase()));

        apiDocs.addNote("Update a tag");
        final String tagComments = randomName();
        post(nexusPath+EP_TAGS+"/"+found.getFirstTag(tag4).getUuid(), toJson(found.getFirstTag(tag4).setCommentary(new EntityCommentary(tagComments))));

        apiDocs.addNote("Lookup Nexus again, verify updated tag");
        found = fromJson(get(nexusPath).json, Nexus.class);
        assertEquals(tagComments, found.getFirstTag(tag4.toLowerCase()).initCommentary().getHeadline());

        apiDocs.addNote("Lookup previous versions, there should now be 2");
        NexusArchive[] archives = fromJson(get(ARCHIVES_ENDPOINT+"/Nexus/"+found.getUuid()).json, NexusArchive[].class);
        assertEquals(2, archives.length);

        apiDocs.addNote("Lookup previous versions of tag we just edited, there should now be 2");
        NexusTagArchive[] tagArchives = fromJson(get(ARCHIVES_ENDPOINT+"/NexusTag/"+found.getFirstTag(tag4.toLowerCase()).getUuid()).json, NexusTagArchive[].class);
        assertEquals(2, tagArchives.length);

        apiDocs.addNote("Delete the nexus");
        delete(nexusPath);

        apiDocs.addNote("Lookup by id, should fail");
        assertEquals(NOT_FOUND, doGet(nexusPath).status);

        apiDocs.addNote("Search again, verify no results");
        searchResults = search(startDate, endDate);
        assertEquals(0, searchResults.count());

        apiDocs.addNote("Verify that tags are still present in the system");
        final String tagUri = TAGS_ENDPOINT + EP_TAG + "/" + tag3;
        Tag tag = fromJson(get(tagUri).json, Tag.class);
        assertEquals(canonicalize(tag3), tag.getCanonicalName());

        apiDocs.addNote("Test resolving several tags at once. Try to resolve 4, only 3 will have a type");
        Tag[] tags = fromJson(post(TAGS_ENDPOINT+"/"+EP_RESOLVE, toJson(new String[] {tag1, tag2, tag3, tag4.toLowerCase()})).json, Tag[].class);
        assertEquals(4, tags.length);
        int numberMissingType = 0;
        for (Tag t : tags) if (!t.hasTagType()) numberMissingType++;
        assertEquals(1, numberMissingType);

        apiDocs.addNote("Find all tag types");
        final TagType[] tagTypes = fromJson(get(TAG_TYPES_ENDPOINT).json, TagType[].class);
        assertEquals(8, tagTypes.length);

        final String autocompleteUri = TAGS_ENDPOINT + EP_AUTOCOMPLETE;
        final String acQuery = "?" + QPARAM_AUTOCOMPLETE + "=f";
        AutocompleteSuggestions autoComplete;

        apiDocs.addNote("Test autocomplete for any tag");
        autoComplete = fromJson(get(autocompleteUri + acQuery).json, AutocompleteSuggestions.class);
        assertEquals(9, autoComplete.getSuggestions().size());

        apiDocs.addNote("Test autocomplete for only event_type tags");
        autoComplete = fromJson(get(autocompleteUri +"/Event+type" + acQuery).json, AutocompleteSuggestions.class);
        assertEquals(4, autoComplete.getSuggestions().size());

        apiDocs.addNote("Test autocomplete for only tags without a type");
        autoComplete = fromJson(get(autocompleteUri +"/" + MATCH_NULL_TYPE + acQuery).json, AutocompleteSuggestions.class);
        assertEquals(1, autoComplete.getSuggestions().size());
    }

    @Test public void testEventTypeAutoTagging () throws Exception {

        Nexus found;  // when we lookup by id
        NexusSummary result; // when we use the search api

        apiDocs.startRecording(DOC_TARGET, "Verify auto-tagging from Nexus.nexusType <-> NexusTag(event_type, name)");

        // define a range of one day of a random year in the distant past
        final TimeRange range = randomTimeRange();
        final String startDate = range.getStartPoint().toString();
        final String endDate = range.getEndPoint().toString();

        final String nexusName = randomName();

        // Create a new Nexus with a random event_type
        final String headline = randomName();
        final Nexus nexus = newNexus(startDate, endDate, nexusName, headline);
        final String nexusType = randomName();
        nexus.setNexusType(nexusType);

        apiDocs.addNote("Define a new Nexus with a nexusType, should automatically create an eventType tag");
        Nexus createdNexus = createNexus(nexusName, nexus);

        final String nexusPath = NEXUS_ENDPOINT + "/" + createdNexus.getUuid();

        apiDocs.addNote("Lookup the Nexus we created by name, verify it has a single event_type tag");
        found = fromJson(get(NEXUS_ENDPOINT+"/"+urlEncode(nexus.getName())).json, Nexus.class);
        assertEquals(nexusName, found.getName());
        assertEquals(nexusType, found.getNexusType());
        assertEquals(1, found.getTags().size());
        assertEquals("event_type", found.getTags().get(0).getTagType());
        assertEquals(nexusType, found.getTags().get(0).getTagName());

        apiDocs.addNote("Update our nexus with a new nexusType, this should create another event_type tag");
        final String updatedType = nexusType + " -- update";
        nexus.setNexusType(updatedType);
        final Nexus updatedNexus = fromJson(post(nexusPath, toJson(nexus)).json, Nexus.class);
        assertEquals(updatedType, updatedNexus.getNexusType());
        assertEquals(2, updatedNexus.getTags().size());
        final List<NexusTag> typeTags = NexusTag.filterByType(updatedNexus.getTags(), "event_type");
        assertEquals(2, typeTags.size());
        assertEquals("event_type", typeTags.get(0).getTagType());
        assertEquals("event_type", typeTags.get(1).getTagType());
        if (typeTags.get(0).getTagName().equals(nexusType)) {
            assertEquals(updatedType, typeTags.get(1).getTagName());
        } else {
            assertEquals(nexusType, typeTags.get(0).getTagName());
        }
    }

}
