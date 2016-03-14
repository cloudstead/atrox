package histori;

import org.junit.Test;

public class SuperNexusTest extends ApiClientTestBase {

    private static final String DOC_TARGET = "SuperNexus validation";

    @Test public void testSuperNexus () throws Exception {
        apiDocs.startRecording(DOC_TARGET, "Add some different versions of a nexus and observe the bounds and range of the SuperNexus change");

        apiDocs.addNote("Add a few versions of a nexus, keeping track of the farthest bounds and range");

        apiDocs.addNote("Look up the supernexus, verify the bounds and range are there, and the right number of versions");

        apiDocs.addNote("Lookup the supernexus with a preferred author list, see authors in that order");


    }

}
