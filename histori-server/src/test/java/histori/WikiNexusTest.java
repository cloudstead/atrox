package histori;

import histori.model.support.NexusRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import static histori.model.TagType.EVENT_TYPE;
import static org.cobbzilla.util.math.Cardinal.*;

@Slf4j
public class WikiNexusTest extends WikiTest {

    public static ArticleNexusExpectedResult[] TESTS = {
            // Test case: A very famous historical battle -- lots of tags to extract
            new ArticleNexusExpectedResult("Battle of Actium")
                    .location(38, 56, 4, north, 20, 44, 19, east)
                    .range("-31-09-02")
                    .tag(EVENT_TYPE, "battle")
                    .tag("event", "Final War of the Roman Republic", "relationship", "part_of")
                    .tag("result", "Decisive Octavian victory")
                    .tag("world_actor", "Octavian's Roman and allied supporters and forces", "role", "combatant")
                    .tag("person", "Marcus Vipsanius Agrippa", "role", "commander", "world_actor", "Octavian's Roman and allied supporters and forces")
                    .tag("person", "Lucius Arruntius", "role", "commander", "world_actor", "Octavian's Roman and allied supporters and forces")
                    .tag("person", "Marcus Lurius", "role", "commander", "world_actor", "Octavian's Roman and allied supporters and forces")
                    .tag("impact", "dead", "estimate", "2500", "world_actor", "Octavian's Roman and allied supporters and forces")
                    .tag("world_actor", "Mark Antony's Roman and allied supporters", "role", "combatant")
                    .tag("world_actor", "Ptolemaic Egypt", "role", "combatant")
                    .tag("person", "Mark Antony", "role", "commander", "world_actor", "Mark Antony's Roman and allied supporters", "world_actor", "Ptolemaic Egypt")
                    .tag("person", "Gaius Sosius", "role", "commander", "world_actor", "Mark Antony's Roman and allied supporters", "world_actor", "Ptolemaic Egypt")
                    .tag("person", "Marcus Octavius (admiral)", "role", "commander", "world_actor", "Mark Antony's Roman and allied supporters", "world_actor", "Ptolemaic Egypt")
                    .tag("person", "Cleopatra VII", "role", "commander", "world_actor", "Mark Antony's Roman and allied supporters", "world_actor", "Ptolemaic Egypt")
                    .tag("impact", "dead", "estimate" , "5000", "world_actor", "Mark Antony's Roman and allied supporters", "world_actor", "Ptolemaic Egypt")
                    .tag("impact", "ships sunk or captured", "estimate" , "200", "world_actor", "Mark Antony's Roman and allied supporters", "world_actor", "Ptolemaic Egypt")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Actium")
                    .markdown("The **Battle of Actium** was the decisive confrontation of the [Final War of the Roman Republic](https://en.wikipedia.org/wiki/Final_War_of_the_Roman_Republic), a naval engagement between [Octavian](https://en.wikipedia.org/wiki/Augustus) and the combined forces of [Mark Antony](https://en.wikipedia.org/wiki/Mark_Antony) and [Cleopatra](https://en.wikipedia.org/wiki/Cleopatra) on 2 September 31 BC, on the [Ionian Sea](https://en.wikipedia.org/wiki/Ionian_Sea) near the city of [Actium](https://en.wikipedia.org/wiki/Actium), in the Roman province of [Epirus Vetus](https://en.wikipedia.org/wiki/Epirus_vetus) in [Greece](https://en.wikipedia.org/wiki/Greece). Octavian's fleet was commanded by [Marcus Vipsanius Agrippa](https://en.wikipedia.org/wiki/Marcus_Vipsanius_Agrippa), while Antony's fleet was supported by the ships of Queen Cleopatra of [Ptolemaic Egypt](https://en.wikipedia.org/wiki/Ptolemaic_Kingdom)."
                        + "\nOctavian's victory enabled him to consolidate his power over Rome and its dominions. He adopted the title of [Princeps](https://en.wikipedia.org/wiki/Princeps) (&quot;first citizen&quot;) and some years later was awarded the title of [Augustus](https://en.wikipedia.org/wiki/Augustus%23First_settlement) (&quot;revered&quot;) by the Roman Senate. This became the name by which he was known in later times. As Augustus, he retained the trappings of a restored Republican leader, but historians generally view this consolidation of power and the adoption of these honorifics as the end of the [Roman Republic](https://en.wikipedia.org/wiki/Roman_Republic) and the beginning of the [Roman Empire](https://en.wikipedia.org/wiki/Roman_Empire)."
                        + "\n\n#### Prelude\n\nThe alliance between Octavian, Antony, and Lepidus, commonly known as the [Second Triumvirate](https://en.wikipedia.org/wiki/Second_Triumvirate), was renewed for a five-year term in 38 BC. However, the triumvirate broke down when Octavian saw [Caesarion](https://en.wikipedia.org/wiki/Caesarion), the professed son of [Julius Caesar](https://en.wikipedia.org/wiki/Julius_Caesar) ... [continue reading](https://en.wikipedia.org/wiki/Battle_of_Actium)"),

            // Test case: a more obscure battle, we must lookup another wiki page to determine the location
            new ArticleNexusExpectedResult("Battle of Purandar")
                    .location(18, 17, north, 73, 59, east)
                    .range("1665")
                    .tag(EVENT_TYPE, "battle")
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
            new ArticleNexusExpectedResult("Battle of Świecino")
                    .location(54.787222, 18.087778)
                    .range("1462-09-17")
                    .tag(EVENT_TYPE, "battle")
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
            new ArticleNexusExpectedResult("Battle of the Bulge")
                    .location(50, 15, north, 5, 40, east)
                    .range("1944-12-16", "1945-01-25")
                    .tag(EVENT_TYPE, "battle")
                    .tag("event", "World War II", "relationship", "part_of")
                    .tag("result", "Allied victory, German operational failure")
                    .tag("world_actor", "United States", "role", "combatant")
                    .tag("world_actor", "United Kingdom", "role", "combatant")
                    .tag("world_actor", "Canada", "role", "combatant")
                    .tag("world_actor", "France", "role", "combatant")
                    .tag("world_actor", "Belgium", "role", "combatant")
                    .tag("world_actor", "Luxembourg", "role", "combatant")
                    .tag("person", "Dwight D. Eisenhower", "role", "commander", "world_actor", "United States")
                    .tag("person", "Bernard Montgomery", "role", "commander", "world_actor", "United Kingdom")
                    .tag("person", "Omar Bradley", "role", "commander", "world_actor", "United States")
                    .tag("person", "Courtney Hodges", "role", "commander", "world_actor", "United States")
                    .tag("person", "George S. Patton", "role", "commander", "world_actor", "United States")
                    .tag("person", "Anthony McAuliffe", "role", "commander", "world_actor", "United States")
                    .tag("impact", "casualties", "estimate", "89500", "world_actor", "United States")
                    .tag("impact", "dead", "estimate", "19000", "world_actor", "United States")
                    .tag("impact", "wounded", "estimate", "47500", "world_actor", "United States")
                    .tag("impact", "captured or missing", "estimate", "23000", "world_actor", "United States")
                    .tag("impact", "tanks/assault guns destroyed", "low_estimate", "700", "estimate", "750", "high_estimate", "800", "world_actor", "United States")
                    .tag("impact", "aircraft destroyed", "estimate", "647", "world_actor", "United States")
                    .tag("impact", "casualties", "estimate", "1408", "world_actor", "United Kingdom")
                    .tag("impact", "dead", "estimate", "200", "world_actor", "United Kingdom")
                    .tag("impact", "wounded", "estimate", "969", "world_actor", "United Kingdom")
                    .tag("impact", "missing", "estimate", "239", "world_actor", "United Kingdom")
                    .tag("world_actor", "Nazi Germany", "role", "combatant")
                    .tag("person", "Adolf Hitler", "role", "commander", "world_actor", "Nazi Germany")
                    .tag("person", "Walter Model", "role", "commander", "world_actor", "Nazi Germany")
                    .tag("person", "Gerd von Rundstedt", "role", "commander", "world_actor", "Nazi Germany")
                    .tag("person", "Hasso von Manteuffel", "role", "commander", "world_actor", "Nazi Germany")
                    .tag("person", "Sepp Dietrich", "role", "commander", "world_actor", "Nazi Germany")
                    .tag("person", "Erich Brandenberger", "role", "commander", "world_actor", "Nazi Germany")
                    .tag("impact", "casualties", "low_estimate", "67459", "estimate", "96229", "high_estimate", "125000", "world_actor", "Nazi Germany")
                    .tag("impact", "dead", "estimate", "10749", "world_actor", "Nazi Germany")
                    .tag("impact", "wounded", "estimate", "34225", "world_actor", "Nazi Germany")
                    .tag("impact", "captured", "estimate", "22487", "world_actor", "Nazi Germany")
                    .tag("impact", "tanks/assault guns destroyed", "low_estimate", "600", "estimate", "700", "high_estimate", "800", "world_actor", "Nazi Germany")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_the_Bulge"),

            // Another one with coordinates that are difficult to find
            new ArticleNexusExpectedResult("Battle of Peleliu", false)
                    .location(7, 0, north, 134, 15, east)
                    .range("1944-09-15", "1944-11-27")
                    .tag(EVENT_TYPE, "battle")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Peleliu"),

            new ArticleNexusExpectedResult("Battle of Waterloo", false)
                    .location(50.68016, 4.41169)
                    .range("1815-06-18")
                    .tag(EVENT_TYPE, "battle")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Waterloo"),

            // Includes wikitables, increased parsing complexity
            new ArticleNexusExpectedResult("Battle of Kadesh", false)
                    .location(34.57, 36.51)
                    .range("-1274-05")
                    .tag(EVENT_TYPE, "battle")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Kadesh"),

            // Uses {{start date}} info block for date, complex casualties stats
            new ArticleNexusExpectedResult("Battle of the Crater", false)
                    .location(37.2183, -77.3777)
                    .range("1864-07-30")
                    .tag(EVENT_TYPE, "battle")
                    .tag("event", "American Civil War", "relationship", "part_of")
                    .tag("result", "Confederate States of America victory")
                    .tag("world_actor", "United States", "role", "combatant")
                    .tag("impact", "casualties", "estimate", "3798", "world_actor", "United States")
                    .tag("impact", "dead", "estimate", "504", "world_actor", "United States")
                    .tag("impact", "wounded", "estimate", "1881", "world_actor", "United States")
                    .tag("impact", "captured or missing", "estimate", "1413", "world_actor", "United States")
                    .tag("world_actor", "Confederate States of America", "role", "combatant")
                    .tag("impact", "casualties", "estimate", "1491", "world_actor", "Confederate States of America")
                    .tag("impact", "dead", "estimate", "361", "world_actor", "Confederate States of America")
                    .tag("impact", "wounded", "estimate", "727", "world_actor", "Confederate States of America")
                    .tag("impact", "captured or missing", "estimate", "403", "world_actor", "Confederate States of America")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_the_Crater"),

            // Had a funky link that was being parsed as a commander, now fixed. Keep to avoid regressing
            new ArticleNexusExpectedResult("Battle of Mortimer's Cross")
                    .location(52, 19, 7, north, 2, 52, 9, west)
                    .range("1461-02-02")
                    .tag(EVENT_TYPE, "battle")
                    .tag("event", "Wars of the Roses", "relationship", "part_of")
                    .tag("result", "Decisive Yorkist victory")
                    .tag("world_actor", "House of York", "role", "combatant")
                    .tag("person", "Edward IV of England", "world_actor", "House of York", "role", "commander")
                    .tag("world_actor", "House of Lancaster", "role", "combatant")
                    .tag("person", "Owen Tudor", "world_actor", "House of Lancaster", "role", "commander")
                    .tag("person", "Jasper Tudor", "world_actor", "House of Lancaster", "role", "commander")
                    .tag("person", "James Butler", "world_actor", "House of Lancaster", "role", "commander")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Mortimer%27s_Cross"),

            // Had a Portuguese flag icon identified as a world_actor, also uses West cardinality, testing geo coordinate parsing
            new ArticleNexusExpectedResult("Battle of Roliça")
                    .location(39.3136, -9.1836)
                    .range("1808-08-17")
                    .tag(EVENT_TYPE, "battle")
                    .tag("event", "Peninsular War", "relationship", "part_of")
                    .tag("result", "Anglo-Portuguese victory, tactical French retreat")
                    .tag("world_actor", "United Kingdom", "role", "combatant")
                    .tag("world_actor", "Portugal", "role", "combatant")
                    .tag("person", "Arthur Wellesley", "role", "commander", "world_actor", "United Kingdom")
                    .tag("impact", "dead and wounded", "estimate", "487", "world_actor", "United Kingdom", "world_actor", "Portugal")
                    .tag("world_actor", "France", "role", "combatant")
                    .tag("world_actor", "Switzerland", "role", "combatant")
                    .tag("person", "Henri Delaborde", "role", "commander", "world_actor", "France")
                    .tag("impact", "dead and wounded", "estimate", "700", "world_actor", "France", "world_actor", "Switzerland")
                    .tag("impact", "guns captured", "estimate", "3", "world_actor", "France", "world_actor", "Switzerland")
                    .tag("citation", "https://en.wikipedia.org/wiki/Battle_of_Roli%C3%A7a"),

            // fixing bug with casualty parsing
            new ArticleNexusExpectedResult("Battle of Chusto-Talasah", false)
                    .location(36.2823, -95.9502)
                    .range("1861-12-09")
                    .tag(EVENT_TYPE, "battle")
                    .tag("impact", "dead", "estimate", "9", "world_actor", "Creek (people)", "world_actor", "Seminole")
                    .tag("impact", "casualties", "estimate", "500", "world_actor", "Creek (people)", "world_actor", "Seminole"),

            // detection of "damaged" casualty that references previous line. complex casualty logic.
            new ArticleNexusExpectedResult("Battle of Britain Day", false)
                    .tag(EVENT_TYPE, "battle")
                    .tag("impact", "aircraft destroyed", "estimate", "29", "world_actor", "United Kingdom")
                    .tag("impact", "aircraft damaged", "estimate", "21", "world_actor", "United Kingdom")
                    .tag("impact", "dead", "low_estimate", "14", "estimate", "15", "high_estimate", "16", "world_actor", "United Kingdom")
                    .tag("impact", "wounded", "estimate", "14", "world_actor", "United Kingdom")
                    .tag("impact", "captured", "estimate", "1", "world_actor", "United Kingdom")
                    .tag("impact", "dead", "low_estimate", "63", "estimate", "72", "high_estimate", "81", "world_actor", "Nazi Germany")
                    .tag("impact", "aircraft destroyed", "low_estimate", "57", "estimate", "59", "high_estimate", "61", "world_actor", "Nazi Germany")
                    .tag("impact", "aircraft damaged", "estimate", "20", "world_actor", "Nazi Germany")
                    .tag("impact", "captured", "low_estimate", "63", "estimate", "64", "high_estimate", "65", "world_actor", "Nazi Germany")
                    .tag("impact", "wounded", "low_estimate", "30", "estimate", "30", "high_estimate", "31", "world_actor", "Nazi Germany")
                    .tag("impact", "missing", "estimate", "21", "world_actor", "Nazi Germany"),

            // test naval casualties
            new ArticleNexusExpectedResult("Naval Battle of Guadalcanal", false)
                    .tag(EVENT_TYPE, "battle")
                    .tag("world_actor", "United States", "role", "combatant")
                    .tag("world_actor", "Empire of Japan", "role", "combatant")
                    .tag("impact", "light cruisers sunk", "estimate", "2", "world_actor", "United States")
                    .tag("impact", "destroyers sunk", "estimate", "3", "world_actor", "United States")
                    .tag("impact", "destroyers sunk", "estimate", "4", "world_actor", "United States")
                    .tag("impact", "aircraft destroyed", "estimate", "36", "world_actor", "United States")
                    .tag("impact", "dead", "estimate", "1732", "world_actor", "United States")
                    .tag("impact", "battleships sunk", "estimate", "1", "world_actor", "Empire of Japan")
                    .tag("impact", "destroyers sunk", "estimate", "2", "world_actor", "Empire of Japan")
                    .tag("impact", "transports lost", "estimate", "7", "world_actor", "Empire of Japan")
                    .tag("impact", "battleships sunk", "estimate", "1", "world_actor", "Empire of Japan")
                    .tag("impact", "destroyers sunk", "estimate", "1", "world_actor", "Empire of Japan")
                    .tag("impact", "transports lost", "estimate", "4", "world_actor", "Empire of Japan")
                    .tag("impact", "aircraft destroyed", "estimate", "64", "world_actor", "Empire of Japan")
                    .tag("impact", "dead", "estimate", "1900", "world_actor", "Empire of Japan"),

            // yet another location coordinate scheme
            new ArticleNexusExpectedResult("Battle of Mount Elba", false)
                    .tag(EVENT_TYPE, "battle")
                    .location(33.0, 46.0, 35.401, north, 93.0, 21.0, 59.619, west)
                    .range("1864-03-30"),

            // article missing cardinal directions
            new ArticleNexusExpectedResult("Battle of Lechfeld (955)", false)
                    .tag(EVENT_TYPE, "battle")
                    .location(48, 22, north, 10, 54, east)
                    .range("955-08-10"),

            // Coord box embedded within place attribute of an infobox
            new ArticleNexusExpectedResult("Battle of Las Navas de Tolosa", false)
                    .tag(EVENT_TYPE, "battle")
                    .location(38.28443, -3.58286)
                    .range("1212-07-16"),

            // infobox with date and location is within a wikitable
            new ArticleNexusExpectedResult("Battle of Evesham", false)
                    .tag(EVENT_TYPE, "battle")
                    .location(52.1058726, -1.9445372)
                    .range("1265-08-04"),

            // unparseable -- not actually a battle (it's a TV show)
            new ArticleNexusExpectedResult("Battle of the Seasons", false).unparseable(true),

            // trouble parsing world_actors
            new ArticleNexusExpectedResult("Battle of Marsaglia", false)
                    .tag(EVENT_TYPE, "battle")
                    .tag("world_actor", "Kingdom of France", "role", "combatant")
                    .tag("world_actor", "Duchy of Savoy", "role", "combatant")
                    .tag("world_actor", "Spain", "role", "combatant"),

            new ArticleNexusExpectedResult("Battle of Ayacucho", false)
                    .tag(EVENT_TYPE, "battle")
                    .tag("world_actor", "Peru", "role", "combatant")
                    .tag("world_actor", "Gran Colombia", "role", "combatant")
                    .tag("world_actor", "Argentina", "role", "combatant")
                    .tag("world_actor", "Chile", "role", "combatant")
                    .tag("world_actor", "Foreign volunteers", "role", "combatant")
                    .tag("world_actor", "British Legions", "role", "combatant")
                    .tag("world_actor", "Monarchy of Spain", "role", "combatant")
                    .tag("world_actor", "Spain", "role", "combatant")
                    .tag("world_actor", "Viceroyalty of Perú", "role", "combatant"),

            new ArticleNexusExpectedResult("Second Punic War", false).unparseable(true),

            new ArticleNexusExpectedResult("Battle of Krasnoi", false)
                    .tag(EVENT_TYPE, "battle")
                    .location(54, 33, 36, north, 31, 25, 48, east)
                    .range("1812-11-15", "1812-11-18"),

            // location cannot be determined
            new ArticleNexusExpectedResult("English Civil War", false).unparseable(true),
    };

    @Test public void testNexusCreationFromWiki() throws Exception {
//        validateCorrectNexus(TESTS[TESTS.length-1]);
//        validateCorrectNexus(TESTS[9]);
//        validateCorrectNexus(findTest("English Civil War"));
        for (ArticleNexusExpectedResult test : TESTS) {
            validateCorrectNexus(test);
        }
    }

    private ArticleNexusExpectedResult findTest(String title) { return findTest(title, TESTS); }

    public void validateCorrectNexus(ArticleNexusExpectedResult test) {
        final NexusRequest nexusRequest = wiki.toNexusRequest(test.title);
        test.verify(nexusRequest);
    }

}
