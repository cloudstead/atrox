package histori;

import histori.dao.SuperNexusDAO;
import histori.model.*;
import histori.model.archive.NexusArchive;
import histori.model.support.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.cobbzilla.wizard.dao.SearchResults;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static histori.ApiConstants.*;
import static histori.model.CanonicalEntity.canonicalize;
import static histori.model.TagType.EVENT_TYPE;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.http.HttpStatusCodes.NOT_FOUND;
import static org.cobbzilla.util.string.StringUtil.urlEncode;
import static org.cobbzilla.util.system.Sleep.sleep;
import static org.cobbzilla.wizardtest.RandomUtil.randomName;
import static org.junit.Assert.*;

@Slf4j
public class NexusTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "Add world events to the map";

    private AccountAuthResponse authResponse;
    private Account account;
    private String password = randomName();

    @Before public void createAccount () throws Exception {
        authResponse = newAdminAccount(password);
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
        final String nexusName = randomName();

        apiDocs.addNote("Search the range ("+range.getStartPoint()+" to "+range.getEndPoint()+"), there should be nothing found");
        SearchResults<NexusSummary> searchResults = search(startDate, endDate, nexusName);
        assertNull(searchResults.getTotalCount());
        assertEquals(0, searchResults.count());
        assertTrue(searchResults.getResults().isEmpty());

        final String tag1 = "War";
        final String tag2 = "USA";
        final String tag3 = "http://example.com/citation.html";

        // Create a new Nexus
        final String markdown = randomName();
        final Nexus nexus = newNexus(startDate, endDate, nexusName, markdown);
        nexus.getTags().addTag(tag1);
        nexus.getTags().addTag(tag2, "world actor");
        nexus.getTags().addTag("United States", "world actor");
        nexus.getTags().addTag("Uprising", "event type");
        nexus.getTags().addTag("US");
        nexus.getTags().addTag(tag3, "citation");

        apiDocs.addNote("Define a new Nexus");
        Nexus createdNexus = createNexus(nexusName, nexus);
        final String nexusPath = NEXUS_ENDPOINT + "/" + urlEncode(createdNexus.getName());

        apiDocs.addNote("Lookup the Nexus we created by name");
        found = get(nexusPath, Nexus.class);
        assertEquals(nexusName, found.getName());
        assertEquals(6, found.getTags().size());
        assertEquals(markdown, found.getMarkdown());

        apiDocs.addNote("Verify that new tags are now present in the system");
        String tagUri = TAGS_ENDPOINT + EP_TAG + "/" + urlEncode("~"+tag2);
        Tag tag = get(tagUri, Tag.class);
        assertEquals(canonicalize(tag2), tag.getCanonicalName());

        apiDocs.addNote("Search for nexus in the same range, should see our new nexus");
        searchResults = search(startDate, endDate, nexusName);
        assertEquals(1, searchResults.count());
        result = searchResults.getResult(0);
        assertEquals(nexusName, result.getPrimary().getName());
        assertEquals(6, result.getPrimary().getTags().size());

        apiDocs.addNote("Update our nexus with new name, this will create an entirely new nexus, because the name is different");
        final String updatedName = nexusName + " -- update";
        nexus.setName(updatedName);
        nexus.setMarkdown(markdown + " -- update");
        final String updatedNexusPath = NEXUS_ENDPOINT + "/" + urlEncode(nexus.getName());
        Nexus updatedNexus = post(updatedNexusPath, nexus);
        assertEquals(updatedName, updatedNexus.getName());
        assertNotEquals(updatedNexus.getUuid(), createdNexus.getUuid());

        apiDocs.addNote("Add another tag - this will create a second version of the nexus");
        String tag4 = randomName(100);
        nexus.getTags().addTag(tag4);
        updatedNexus = post(updatedNexusPath, nexus);

        apiDocs.addNote("Lookup the Nexus we updated by uuid, verify updated changes");
        found = get(updatedNexusPath, Nexus.class);
        assertEquals(updatedName, found.getName());
        assertEquals(7, found.getTags().size());
        assertTrue(found.getTags().hasTag(tag4));
        assertTrue(found.getTags().hasTag(tag4.toLowerCase()));

        apiDocs.addNote("Update a tag - this will create a third version of the nexus");
        final String tagComments = randomName();
        found.getTags().getFirstTag(tag4).setValue("meta", tagComments);
        updatedNexus = post(updatedNexusPath, found);

        apiDocs.addNote("Lookup Nexus again, verify updated tag");
        found = get(updatedNexusPath, Nexus.class);
        assertEquals(tagComments, found.getTags().getFirstTag(tag4.toLowerCase()).getSchemaValueMap().get("meta"));

        apiDocs.addNote("Lookup previous versions, there should now be 3");
        NexusArchive[] archives = get(ARCHIVES_ENDPOINT+"/Nexus/"+found.getUuid(), NexusArchive[].class);
        assertEquals(3, archives.length);

        apiDocs.addNote("Lookup based on matching tag value");
        searchResults = search(startDate, endDate, "fuzzy:tag-name:"+tag4.substring(tag4.length()/2));
        assertEquals(1, searchResults.count());

        apiDocs.addNote("Lookup based on matching tag value, using alternate syntax");
        searchResults = search(startDate, endDate, "fuzzy:tag-name:\""+tag4.substring(tag4.length()/2)+"\"");
        assertEquals(1, searchResults.count());

        apiDocs.addNote("Lookup based on matching decorator value");
        searchResults = search(startDate, endDate, "exact:decorator-value:"+tagComments);
        assertEquals(1, searchResults.count());

        apiDocs.addNote("Lookup based on matching anything in tags");
        searchResults = search(startDate, endDate, "tags:"+tag4);
        assertEquals(1, searchResults.count());

        apiDocs.addNote("Delete the nexus");
        delete(updatedNexusPath);

        apiDocs.addNote("Lookup by id, should fail");
        assertEquals(NOT_FOUND, doGet(updatedNexusPath).status);

        apiDocs.addNote("Delete the original nexus");
        delete(nexusPath);

        apiDocs.addNote("Lookup original by id, should fail");
        assertEquals(NOT_FOUND, doGet(nexusPath).status);

        // force SuperNexusDAO to refresh dirty records
        long start = now();
        final SuperNexusDAO superNexusDAO = getBean(SuperNexusDAO.class);
        superNexusDAO.forceRefresh();
        long timeout = TimeUnit.SECONDS.toMillis(5);
        while (superNexusDAO.oldestRefreshTime() < start) {
            assertFalse("timed out waiting for SuperNexusDAO to refresh", now() > start + timeout);
            sleep(100);
        }

        apiDocs.addNote("Search again, verify no results");
        searchResults = search(startDate, endDate, updatedName);
        assertEquals(0, searchResults.count());

        apiDocs.addNote("Verify that tags are still present in the system");
        tagUri = TAGS_ENDPOINT + EP_TAG + "/" + urlEncode("~"+urlEncode(tag3));
        tag = get(tagUri, Tag.class);
        assertEquals(canonicalize(tag3), tag.getCanonicalName());

        apiDocs.addNote("Test resolving several tags at once. Try to resolve 4, only 3 will have a type");
        Tag[] tags = post(TAGS_ENDPOINT+"/"+EP_RESOLVE, new String[] {tag1, tag2, tag3, tag4.toLowerCase()}, Tag[].class);
        assertEquals(4, tags.length);
        int numberMissingType = 0;
        for (Tag t : tags) if (!t.hasTagType()) numberMissingType++;
        assertEquals("wrong number of missing tags. tags="+ ArrayUtils.toString(tags), 2, numberMissingType);

        apiDocs.addNote("Find all tag types");
        final TagType[] tagTypes = get(TAG_TYPES_ENDPOINT, TagType[].class);
        assertEquals(9, tagTypes.length);

        final String autocompleteUri = TAGS_ENDPOINT + EP_AUTOCOMPLETE;
        final String acQuery = "?" + QPARAM_AUTOCOMPLETE + "=u";
        AutocompleteSuggestions autoComplete;

        apiDocs.addNote("Test autocomplete for any tag");
        autoComplete = get(autocompleteUri + acQuery, AutocompleteSuggestions.class);
        assertEquals(4, autoComplete.getSuggestions().size());

        apiDocs.addNote("Test autocomplete for only event_type tags");
        autoComplete = get(autocompleteUri +"/Event_type" + acQuery, AutocompleteSuggestions.class);
        assertEquals(1, autoComplete.getSuggestions().size());

        apiDocs.addNote("Test autocomplete for only tags without a type");
        autoComplete = get(autocompleteUri +"/" + MATCH_NULL_TYPE + acQuery, AutocompleteSuggestions.class);
        assertEquals(1, autoComplete.getSuggestions().size());
    }

    @Test public void testEventTypeAutoTagging () throws Exception {

        Nexus found;  // when we lookup by id

        apiDocs.startRecording(DOC_TARGET, "Verify auto-tagging from Nexus.nexusType <-> NexusTag(event_type, name)");

        // define a range of one day of a random year in the distant past
        final TimeRange range = randomTimeRange();
        final String startDate = range.getStartPoint().toString();
        final String endDate = range.getEndPoint().toString();

        final String nexusName = "x-"+randomName();

        // Create a new Nexus with a random event_type
        final String markdown = randomName();
        final Nexus nexus = newNexus(startDate, endDate, nexusName, markdown);
        final String nexusType = "x-"+randomName();
        nexus.setNexusType(nexusType);

        apiDocs.addNote("Define a new Nexus with a nexusType, should automatically create an eventType tag");
        Nexus createdNexus = createNexus(nexusName, nexus);

        apiDocs.addNote("Lookup the Nexus we created by name, verify it has a single event_type tag");
        found = get(NEXUS_ENDPOINT+"/"+urlEncode(nexus.getName()), Nexus.class);
        assertEquals(nexusName, found.getName());
        assertEquals(nexusType, found.getNexusType());
        assertEquals(1, found.getTags().size());
        assertEquals(EVENT_TYPE, found.getTags().get(0).getTagType());
        assertEquals(nexusType, found.getTags().get(0).getTagName());

        apiDocs.addNote("Update our nexus with a new nexusType, this should create another event_type tag");
        final String updatedType = nexusType + " -- update";
        createdNexus.setNexusType(updatedType);
        final Nexus updatedNexus = post(NEXUS_ENDPOINT+"/"+urlEncode(nexusName)+"/"+createdNexus.getUuid(), createdNexus);
        assertEquals(updatedType, updatedNexus.getNexusType());
        assertEquals(2, updatedNexus.getTags().size());
        final List<NexusTag> typeTags = NexusTag.filterByType(updatedNexus.getTags(), EVENT_TYPE);
        assertEquals(2, typeTags.size());
        assertEquals(EVENT_TYPE, typeTags.get(0).getTagType());
        assertEquals(EVENT_TYPE, typeTags.get(1).getTagType());
        if (typeTags.get(0).getTagName().equals(nexusType)) {
            assertEquals(updatedType, typeTags.get(1).getTagName());
        } else {
            assertEquals(nexusType, typeTags.get(0).getTagName());
        }
    }

    @Test public void testCopyOnWrite () throws Exception {

        apiDocs.startRecording(DOC_TARGET, "Edit someone else's nexus, this creates a copy");

        // define a range of one day of a random year in the distant past
        final TimeRange range = randomTimeRange();
        final String startDate = range.getStartPoint().toString();
        final String endDate = range.getEndPoint().toString();

        final String nexusName = randomName();

        // Create a new Nexus with a random event_type
        final String markdown = randomName();
        final Nexus nexus = newNexus(startDate, endDate, nexusName, markdown);
        final String nexusType = randomName();
        nexus.setNexusType(nexusType);

        apiDocs.addNote("Define a new Nexus with a nexusType, should automatically create an eventType tag");
        Nexus createdNexus = createNexus(nexusName, nexus);

        apiDocs.addNote("Register another user account");
        newAdminAccount();

        apiDocs.addNote("Edit the nexus created by the first user, verify we get a copy");
        final String nexusPath = NEXUS_ENDPOINT + "/" + createdNexus.getName();
        final String altMarkdown = randomName();
        createdNexus.setMarkdown(altMarkdown);
        createdNexus.setVisibility(EntityVisibility.everyone);
        final Nexus updatedNexus = post(nexusPath, createdNexus);
        assertNotEquals(createdNexus.getUuid(), updatedNexus.getUuid());
        assertEquals(altMarkdown, updatedNexus.getMarkdown());

        apiDocs.addNote("Do search, we should see a single summary with both versions");
        SearchResults<NexusSummary> results = search(startDate, endDate, nexusName);
        assertEquals(1, results.getResults().size());
        assertNotNull(results.getResult(0).getOthers());
        assertEquals(1, results.getResult(0).getOthers().length);
        log.info("results="+results);
    }

}
