package histori;

import com.fasterxml.jackson.databind.JsonNode;
import histori.model.Nexus;
import histori.model.QueryBackend;
import histori.model.SearchQuery;
import histori.model.support.NexusSummary;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.DebugSearchQuery;
import org.cobbzilla.wizard.dao.SearchResults;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static histori.ApiConstants.EP_QUERY;
import static histori.ApiConstants.MAX_SEARCH_TIMEOUT;
import static histori.ApiConstants.SEARCH_ENDPOINT;
import static histori.model.support.NexusSummary.SEARCH_RESULT_TYPE;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsString;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.junit.Assert.assertEquals;

@Slf4j
public class SearchTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "Search Tests";

    @Test
    public void testSearch() throws Exception {
        apiDocs.startRecording(DOC_TARGET, "test searching");
        final DebugSearchQuery query = new DebugSearchQuery()
                .setQuery(loadResourceAsString("search/test-query1.json"))
                .setFilter(loadResourceAsString("search/test-filter1.json"))
//                .setSource(loadResourceAsString("search/test-search1.json"))
                .setFrom(0)
                .setMaxResults(100);
        apiDocs.addNote("run a search");
        final JsonNode response = fromJson(post(SEARCH_ENDPOINT + "/debug", toJson(query)).json, JsonNode.class);
        final List<Nexus> results = new ArrayList<>();
        for (JsonNode node : response.get("hits").get("hits")) {
            results.add(fromJson(node, "source", Nexus.class));
        }
        log.info("testSearch: got response results=" + toJson(results));
    }

    @Test public void testCompareSearches() throws Exception {
        apiDocs.startRecording(DOC_TARGET, "test searching against both backends");
        SearchQuery query = new SearchQuery()
                .setQuery("Battle of Waterloo")
                .setBounds(80, -80, 180, -180)
                .setRange("1000", "2000")
                .setTimeout(MAX_SEARCH_TIMEOUT) // useful for interactive debugging
                .setUseCache(false);
        apiDocs.addNote("query elastic search");
//        final SearchResults<NexusSummary> esResults = fromJson(post(SEARCH_ENDPOINT + EP_QUERY, toJson(query)).json, SEARCH_RESULT_TYPE);

        apiDocs.addNote("query shards");
        query.setBackend(QueryBackend.pg);
        final SearchResults<NexusSummary> pgResults = fromJson(post(SEARCH_ENDPOINT + EP_QUERY, toJson(query)).json, SEARCH_RESULT_TYPE);

        log.info("got: "+pgResults);
//        assertListsEqual(esResults.getResults(), pgResults.getResults());
    }

    private void assertListsEqual(List list1, List list2) {
        assertEquals(list1.size(), list2.size());
        for (int i=0; i<list1.size(); i++) {
            assertEquals(list1.get(i), list2.get(i));
        }
    }
}