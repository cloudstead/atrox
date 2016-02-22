package histori;

import cloudos.service.asset.ResourceStorageService;
import histori.model.NexusTag;
import histori.model.support.LatLon;
import histori.model.support.NexusRequest;
import histori.model.support.TimeRange;
import histori.model.tag_schema.TagSchemaValue;
import histori.wiki.WikiArchive;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.math.Cardinal;
import org.geojson.Point;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.cobbzilla.util.math.Cardinal.east;
import static org.cobbzilla.util.math.Cardinal.north;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@Slf4j
public class WikiNexusTest {

    private WikiArchive wiki = new WikiArchive(new ResourceStorageService("wiki/index"));

    public static TestPage[] TESTS = {
            // Test case: A very famous historical battle -- lots of tags to extract
            new TestPage("Battle of Actium")
                    .location(38, 56, 4, north, 20, 44, 19, east)
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
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Actium"),

            // Test case: a more obscure battle, we must lookup another wiki page to determine the location
            new TestPage("Battle of Purandar")
                    .location(18, 17, north, 73, 59, east)
                    .range("1665")
                    .tag("event_type", "battle")
                    .tag("event", "Imperial Maratha Conquests", "relationship", "part_of")
                    .tag("result", "Mughal Victory. Shivaji surrenders.")
                    .tag("world_actor", "Maratha Empire", "role", "combatant")
                    .tag("person", "Shivaji", "role", "commander", "world_actor", "Maratha Empire")
                    .tag("person", "Murarbaji Deshpande", "role", "commander", "world_actor", "Maratha Empire")
                    .tag("world_actor", "Mughal Empire", "role", "combatant")
                    .tag("person", "Dilir Khan", "role", "commander", "world_actor", "Mughal Empire")
                    .tag("person", "Mirza Jai Singh", "role", "commander", "world_actor", "Mughal Empire")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Purandar"),

            // A page title with non-ASCII characters, and 'commanders' that contain special characters
            new TestPage("Battle of Świecino")
                    .location(54.787222, 18.087778)
                    .range("1462-09-17")
                    .tag("event_type", "battle")
                    .tag("event", "Thirteen Years' War (1454–66)", "relationship", "part_of")
                    .tag("result", "Decisive Polish Victory")
                    .tag("world_actor", "Teutonic Order", "role", "combatant")
                    .tag("person", "Fritz Raweneck", "role", "commander", "world_actor", "Teutonic Order")
                    .tag("person", "Kaspar Nostyc", "role", "commander", "world_actor", "Teutonic Order")
                    .tag("impact", "dead", "estimate", "1000", "world_actor", "Teutonic Order")
                    .tag("impact", "captured", "estimate", "50", "world_actor", "Teutonic Order")
                    .tag("world_actor", "Poland", "role", "combatant")
                    .tag("person", "Piotr Dunin", "role", "commander", "world_actor", "Poland")
                    .tag("impact", "dead", "estimate", "250", "world_actor", "Poland")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_%C5%9Awiecino"),

            // Another big battle with lots of data to extract and a very large document (>100KB)
            // Also a very complex infobox of combatants and commanders
            new TestPage("Battle of the Bulge")
                    .location(50, 15, north, 5, 40, east)
                    .range("1944-12-16", "1945-01-25")
                    .tag("event_type", "battle")
                    .tag("event", "World War II", "relationship", "part_of")
                    .tag("result", "Allied victory, German operational failure")
                    .tag("world_actor", "United States", "role", "combatant")
                    .tag("world_actor", "United Kingdom", "role", "combatant")
                    .tag("world_actor", "Provisional Government of the French Republic", "role", "combatant")
                    .tag("world_actor", "Belgium", "role", "combatant")
                    .tag("world_actor", "Luxembourg Resistance", "role", "combatant")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_the_Bulge"),
    };

    @Test public void testNexusCreationFromWiki() throws Exception {
        validateCorrectNexus(TESTS[TESTS.length-1]);
        for (TestPage test : TESTS) {
            validateCorrectNexus(test);
        }
    }

    public void validateCorrectNexus(TestPage test) {
        final NexusRequest nexusRequest = wiki.toNexusRequest(test.title);
        assertNotNull("error parsing article: "+test.title, nexusRequest);
        assertEquals(test.getGeoJson(), nexusRequest.getGeoJson());
        assertEquals(test.range, nexusRequest.getTimeRange());
        assertEquals(test.tags.size(), nexusRequest.getTagCount());
        for (NexusTag tag : test.tags) {
            assertTrue("missing tag: "+tag.getTagName(), nexusRequest.hasTag(tag.getTagName()));
            assertTrue("tag doesn't match: "+tag.getTagName(), isSameTag(tag, nexusRequest.getTag(tag.getTagName())));
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
        public TestPage location (double lat, double lon) { return location(new LatLon(lat, lon)); }
        public TestPage location (int latDeg, Integer latMin, Integer latSec, Cardinal latDir, int lonDeg, Integer lonMin, Integer lonSec, Cardinal lonDir) {
            this.location = new LatLon(latDeg, latMin, latSec, latDir, lonDeg, lonMin, lonSec, lonDir);
            return this;
        }
        public TestPage location (int latDeg, Integer latMin, Cardinal latDir, int lonDeg, Integer lonMin, Cardinal lonDir) {
            return location(latDeg, latMin, null, latDir, lonDeg, lonMin, null, lonDir);
        }
        public String getGeoJson() { return toJsonOrDie(new Point(location.getLon(), location.getLat())); }

        public TimeRange range;
        public TestPage range(String date) { this.range = new TimeRange(date); return this; }
        public TestPage range(String start, String end) { this.range = new TimeRange(start, end); return this; }

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
