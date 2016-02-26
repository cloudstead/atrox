package histori;

import histori.model.support.MultiNexusRequest;
import histori.model.support.NexusRequest;
import org.junit.Test;

import static org.cobbzilla.util.math.Cardinal.north;
import static org.cobbzilla.util.math.Cardinal.west;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class WikiNexusMultiTest extends WikiTest {

    static ArticleNexusExpectedResult[] TESTS = {

            new ArticleNexusExpectedResult("London")

                .nexus(new ArticleNexusExpectedResult("London Settled by Roman Empire")
                    .location(51, 30, 26, north, 0, 7, 39, west)
                    .range("43")
                    .tag("event_type", "founding")
                    .tag("world_actor", "Roman Empire", "role", "founder")
                    .tag("citation", "https://en.wikipedia.org/wiki/London")),

            new ArticleNexusExpectedResult("San Francisco")

                    .nexus(new ArticleNexusExpectedResult("San Francisco Mission")
                            .location(37, 47, north, 122, 25, west)
                            .range("1776-06-29")
                            .tag("event_type", "founding")
                            .tag("citation", "https://en.wikipedia.org/wiki/San_Francisco"))

                    .nexus(new ArticleNexusExpectedResult("San Francisco Municipal incorporation")
                    .location(37, 47, north, 122, 25, west)
                    .range("1850-04-15")
                    .tag("event_type", "founding")
                    .tag("citation", "https://en.wikipedia.org/wiki/San_Francisco")),

    };

    @Test public void testNexusCreationFromWiki() throws Exception {
//        validateCorrectNexus(TESTS[TESTS.length-1]);
//        validateCorrectNexus(TESTS[9]);
        for (ArticleNexusExpectedResult test : TESTS) {
            validateCorrectNexuses(test);
        }
    }

    public void validateCorrectNexuses(ArticleNexusExpectedResult test) {

        final NexusRequest nexusRequest = wiki.toNexusRequest(test.title);
        assertNotNull(nexusRequest);
        if (!(nexusRequest instanceof MultiNexusRequest)) fail("Expected a "+MultiNexusRequest.class.getName());

        final MultiNexusRequest multi = (MultiNexusRequest) nexusRequest;
        for (NexusRequest request : multi.getRequests()) {
            ArticleNexusExpectedResult expectedRequest = test.getExpectedRequest(request.getName());
            assertNotNull("Result expected for: "+request.getName(), expectedRequest);
            expectedRequest.verify(request);
        }
    }


}
