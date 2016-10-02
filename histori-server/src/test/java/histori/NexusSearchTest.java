package histori;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.client.script.ApiRunner;
import org.junit.Test;

import static org.cobbzilla.util.io.StreamUtil.stream2string;

@Slf4j
public class NexusSearchTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "Add nexus that overrides an authoritative one. Ensure searches are accurate.";

    @Getter(lazy=true) private final ApiRunner apiRunner = initApiRunner();
    private ApiRunner initApiRunner() { return new ApiRunner(this, apiDocsRunnerListener); }

    @Test public void testEditAuthoritativeNexus () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "edit an authoritative nexus, verify subsequent searches are accurate as seen by various users");
        getApiRunner().run(stream2string("tests/edit_authoritative.json"));
    }

}
