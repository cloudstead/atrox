package histori;

import histori.model.support.TimeRange;
import histori.wiki.WikiDateFormat;
import org.junit.Test;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.junit.Assert.fail;

public class WikiDateFormatTest {

    public static final String[][] TESTS = new String[][]{
            {"19 July 1870&amp;nbsp;– 10 May 1871&lt;br /&gt;( age in years, months, weeks and days month1 07 day1 19 year1 1870 month2 05 day2 10 year2 1871 )", "1870-07-19_1871-05-10"},
            {"11 September 1865<ref name=\"Bancroft\" />{{rp", "1865-09-11"},
            {"1223–1240", "1223_1240"},
            {"circa September, 9 Common Era C.E.", "9-09"},
            {"1835–36", "1835_1836"},
            {"218–201 BC", "-218_-201"},
            {"2 April spaced ndash 14 June 1982", "1982-04-02_1982-06-14"},
            {"264–241 BC", "-264_-241"},
            {"circa 43 AD (as Londinium )", "43"},
            {" Late May 1274 BC<ref>Lorna Oakes, Pyramids, Temples & Tombs of Ancient Egypt: An Illustrated Atlas of the Land of the Pharaohs, Hermes House: 2003. P. 142.</ref>", "-1274-05"},
            {"16 December 1944 – 25 January 1945", "1944-12-16_1945-01-25"},
            {"15 September – 27 November 1944", "1944-09-15_1944-11-27"},
            {"5–6 June 1967", "1967-06-05_1967-06-06"},
            {"September 2, 31 BC", "-31-09-02"},
            {"July or August, 251", "251-07"},
            {"May 2014", "2014-05"},
            {"February 7, 1866", "1866-02-07"},
            {"2005-04-03 ", "2005-04-03"},
            {"17 January 1885", "1885-01-17"},
            {" November 411 BC", "-411-11"},
            {"10–12 January 1863<ref name=\"Sutter\" />{{rp", "1863-01-10_1863-01-12"},
            {"3 June 1864", "1864-06-03"},
            {"20 August 917", "917-08-20"},
            {" 18–19 January 1991<br>(", "1991-01-18_1991-01-19"},
            {"January 22–24, 1599", "1599-01-22_1599-01-24"},
            {"2010", "2010"},
            {"September 13, 533", "533-09-13"},
            {" {{Start date", null},
            {"September 2008", "2008-09"},
            {"1254", "1254"},
            {"9 August 378", "378-08-09"},
            {" 1 March 1896", "1896-03-01"},
            {"Early 255 BC", "-255"},
            {" Summer 217 BC", "-217"},
            {"28 May 2011 ", "2011-05-28"},
            {"June 2013", "2013-06"},
            {"4 May 1823", "1823-05-04"},
            {"26–27 February 1991", "1991-02-26_1991-02-27"},
            {"Autumn 1997 ", "1997"},
            {"January 18, 1546", "1546-01-18"},
            {"March 9–10, 1966", "1966-03-09_1966-03-10"},
            {"June 2013", "2013-06"},
            {" 2–21 October 1944", "1944-10-02_1944-10-21"},
            {"August 2014", "2014-08"},
            {"1511 C.E.", "1511"},
            {"July 12, 1537", "1537-07-12"},
            {"September 2011", "2011-09"},
            {"1990 ", "1990"},
            {"20 April 1809", "1809-04-20"},
            {"April 2012", "2012-04"},
            {"13 September 1644", "1644-09-13"},
    };

    @Test public void testDateParsing () throws Exception {
        int failCount = 0;
        int totalCount = 0;
        StringBuilder failures = new StringBuilder();
        for (String[] test : TESTS) {
            totalCount++;
            final String input = test[0];
            final String expected = test[1];
            final TimeRange times;
            boolean parsedOk = false;
            String output = null;
            try {
                times = WikiDateFormat.parse(input);
                parsedOk = true;
                output = times.toString();
                if (expected != null && !output.equals(expected)) {
                    die("expected "+input+" -> "+expected+" but got "+output);
                } else if (expected == null) {
                    die("expected "+input+" -> FAIL, but got "+output);
                }

            } catch (Exception e) {
                // a null expected value means that we expect parsing to fail
                if (expected != null || parsedOk) {
                    failures.append("\ninput=" + input+", expected="+expected+", parsed="+output);
                    failCount++;
                }
            }
        }
        if (failures.length() > 0) {
            failures.append("\n\ndate parsing tests: "+totalCount);
            failures.append("\nfailed date parsing tests: "+failCount);
            fail(failures.toString());
        }
    }

}
