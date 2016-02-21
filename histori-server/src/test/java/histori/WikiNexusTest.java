package histori;

import cloudos.service.asset.ResourceStorageService;
import histori.model.NexusTag;
import histori.model.support.LatLon;
import histori.model.support.NexusRequest;
import histori.model.support.TimeRange;
import histori.model.tag_schema.TagSchemaValue;
import histori.wiki.WikiArchive;
import org.cobbzilla.util.math.Cardinal;
import org.geojson.Point;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WikiNexusTest {

    private WikiArchive wiki = new WikiArchive(new ResourceStorageService("wiki/index"));

    public static TestPage[] TESTS = {
            // Test case: A very famous historical battle -- lots of tags to extract
            new TestPage("Battle of Actium")
                    .location(new LatLon(38.0, 56.0, 4.0, Cardinal.north, 20.0, 44.0, 19.0, Cardinal.east))
                    .range("-31-09-02")
                    .tag("event_type", "battle")
                    .tag("event", "Final War of the Roman Republic", "relationship", "part_of")
                    .tag("result", "Decisive Octavian victory")
                    .tag("world_actor", "Octavian's Roman and allied supporters and forces", "role", "combatant")
                    .tag("person", "Marcus Vipsanius Agrippa", "role", "commander", "world_actor", "Octavian's Roman and allied supporters and forces")
                    .tag("person", "Lucius Arruntius", "role", "commander", "world_actor", "Octavian's Roman and allied supporters and forces")
                    .tag("person", "Marcus Lurius", "role", "commander", "world_actor", "Octavian's Roman and allied supporters and forces")
                    .tag("impact", "dead", "estimate", "2500", "world_actor", "Octavian's Roman and allied supporters and forces")
                    .tag("world_actor", "Mark Antony's Roman and allied supporters", "role", "combatant")
                    .tag("world_actor", "Ptolemaic Egypt", "role", "combatant")
                    .tag("person", "Mark Antony", "role", "commander", "world_actor", "Octavian's Roman and allied supporters and forces", "world_actor", "Ptolemaic Egypt")
                    .tag("person", "Gaius Sosius", "role", "commander", "world_actor", "Octavian's Roman and allied supporters and forces", "world_actor", "Ptolemaic Egypt")
                    .tag("person", "Marcus Octavius (admiral)", "role", "commander", "world_actor", "Octavian's Roman and allied supporters and forces", "world_actor", "Ptolemaic Egypt")
                    .tag("person", "Cleopatra VII", "role", "commander", "world_actor", "Octavian's Roman and allied supporters and forces", "world_actor", "Ptolemaic Egypt")
                    .tag("impact", "dead", "estimate" , "5000", "world_actor", "Octavian's Roman and allied supporters and forces", "world_actor", "Ptolemaic Egypt")
                    .tag("impact", "ships sunk or captured", "estimate" , "200", "world_actor", "Octavian's Roman and allied supporters and forces", "world_actor", "Ptolemaic Egypt")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle+of+Actium")
            
    };

    @Test public void testActium () throws Exception {
        for (TestPage test : TESTS) {
            final NexusRequest nexusRequest = wiki.toNexusRequest(test.title);
            assertEquals(test.getGeoJson(), nexusRequest.getGeoJson());
            assertEquals(test.range, nexusRequest.getTimeRange());
            assertEquals(test.tags.size(), nexusRequest.getTagCount());
            for (NexusTag tag : test.tags) {
                assertTrue("missing tag: "+tag.getTagName(), nexusRequest.hasTag(tag.getTagName()));
                assertTrue("tag doesn't match: "+tag.getTagName(), isSameTag(tag, nexusRequest.getTag(tag.getTagName())));
            }
        }
    }

    private boolean isSameTag(NexusTag t1, NexusTag t2) {
        if (!t1.getTagType().equalsIgnoreCase(t2.getTagType())) return false;
        if (!t1.getTagName().equalsIgnoreCase(t2.getTagName())) return false;
        if (!t1.hasSchemaValues()) return !t2.hasSchemaValues();

        final TagSchemaValue[] t1schema = t1.getValues();
        final TagSchemaValue[] t2schema = t1.getValues();
        if (t1schema.length != t2schema.length) return false;

        for (TagSchemaValue t1val : t1schema) {
            boolean found = false;
            for (TagSchemaValue t2val : t2schema) {
                if (t1val.getField().equals(t2val.getField())
                        && t1val.getValue().equals(t2val.getValue())) {
                    found = true; break;
                }
            }
            if (!found) return false;
        }

        return true;
    }

    private static class TestPage {

        public String title;
        public TestPage (String title) { this.title = title; }

        public LatLon location;
        public TestPage location (LatLon location) { this.location = location; return this; }
        public String getGeoJson() { return toJsonOrDie(new Point(location.getLon(), location.getLat())); }

        public TimeRange range;
        public TestPage range(String date) { this.range = new TimeRange(date); return this; }

        public List<NexusTag> tags = new ArrayList<>();
        public TestPage tag(String tagType, String tagName) {
            tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName));
            return this;
        }
        public TestPage tag(String tagType, String tagName, String field1, String value1) {
            tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName).setValue(field1, value1));
            return this;
        }
        public TestPage tag(String tagType, String tagName, String field1, String value1, String field2, String value2) {
            tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName).setValue(field1, value1).setValue(field2, value2));
            return this;
        }
        public TestPage tag(String tagType, String tagName, String field1, String value1, String field2, String value2, String field3, String value3) {
            tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName).setValue(field1, value1).setValue(field2, value2).setValue(field3, value3));
            return this;
        }
    }
}
