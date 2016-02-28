package histori;

import histori.model.support.TimeRange;
import histori.wiki.finder.TextEventFinder;
import histori.wiki.finder.impl.MetroFinder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TextEventFinderTest {

    public static String[][] TESTS = new String[][] {{
            "==History==\nThe first European-American settler, Tenney Peabody, arrived in 1833 along with his brother-in-law Charles Blanchard",
            "1833"
    }, {
            "Eskilstuna Municipality''' (''Eskilstuna kommun'') is a [[municipalities of Sweden|municipality]] in [[Södermanland County]] in southeast [[Sweden]], between the [[lake]]s [[Mälaren]] and [[Hjälmaren]]. Its seat is located in the [[city status in Sweden|city]] of [[Eskilstuna]].\n\nThe present municipality was formed in 1971 when the ''City of Eskilstuna'', the ''City of [[Torshälla]]'' and five rural municipalities were amalgamated.\n",
            "1971"
    }, {
            "Glasnevin seems to have been founded by [[Mobhí Clárainech|Saint Mobhi]] (sometimes known as St Berchan) in the sixth (or perhaps fifth) century as a monastery. ",
            "500"
    }, {
            "In 1971 Motala Municipality was formed by the ",
            "1971"
    }};

    @Test public void testTextFinders () {
        for (String[] test : TESTS) {
            boolean ok = false;
            for (TextEventFinder finder : MetroFinder.TEXT_FINDERS) {
                TimeRange found = finder.find(test[0]);
                if (found != null) {
                    assertEquals(test[1], found.toString());
                    ok = true;
                    break;
                }
            }
            if (!ok) fail("no finder matched: "+test[0]);
        }
    }
}
