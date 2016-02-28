package histori;

import histori.model.support.TimeRange;
import histori.wiki.finder.TextEventFinder;
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
    }, {
            "\n\nSince the 19th century, the built-up area of Paris has grown far beyond its administrative borders; together with its suburbs, the whole agglomeration has a population of 10,550,350 (Jan. 2012 census).[2] Paris' metropolitan area spans most of the Paris region and has a population of 12,341,418 (Jan. 2012 census),[3] or one-fifth of the population of France.[6] The administrative region covers 12,012 km² (4,638 mi²), with approximately 12 million inhabitants as of 2014, and has its own regional council and president.[7]\n\nParis was founded in the 3rd century BC by a Celtic people called the Parisii, who gave the city its name. By the 12th century, Paris was the largest city in the western world, a prosperous trading centre, and the home of the University of Paris, one of the first in Europe. In the 18th century, it was the centre stage for the French Revolution, and became an important centre of finance, commerce, fashion, science, and the arts, a position it still retains today.",
            "200"
    }};

    @Test public void testTextFinders () {
        findText(TESTS[TESTS.length-1]);
        for (String[] test : TESTS) {
            findText(test);
        }
    }

    public void findText(String[] test) {
        boolean ok = false;
        for (TextEventFinder finder : TextEventFinder.FINDERS) {
            TimeRange found = finder.findRange(test[0]);
            if (found != null) {
                assertEquals(test[1], found.toString());
                ok = true;
                break;
            }
        }
        if (!ok) fail("no finder matched: "+test[0]);
    }
}
