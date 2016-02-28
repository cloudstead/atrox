package histori.wiki;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class OrdinalCentury {

    private static Map<String, Integer> ordinalMap = new LinkedHashMap<>();
    private static List<String> ordinals = new ArrayList<>();
    static {
        ordinalMap.put("first", 1);
        ordinalMap.put("second", 100);
        ordinalMap.put("third", 200);
        ordinalMap.put("fourth", 300);
        ordinalMap.put("fifth", 400);
        ordinalMap.put("sixth", 500);
        ordinalMap.put("seventh", 600);
        ordinalMap.put("eighth", 700);
        ordinalMap.put("ninth", 800);
        ordinalMap.put("tenth", 900);
        ordinalMap.put("eleventh", 1000);
        ordinalMap.put("twelfth", 1100);
        ordinalMap.put("thirteenth", 1200);
        ordinalMap.put("fourteenth", 1300);
        ordinalMap.put("fifteenth", 1400);
        ordinalMap.put("sixteenth", 1500);
        ordinalMap.put("seventeenth", 1600);
        ordinalMap.put("eighteenth", 1700);
        ordinalMap.put("nineteenth", 1800);
        ordinalMap.put("twentieth", 1900);
        ordinalMap.put("twenty-first", 2000);
        ordinalMap.put("twenty-second", 2100);
        ordinalMap.put("twenty-third", 2200);
        ordinalMap.put("twenty-fourth", 2300);
        ordinalMap.put("twenty-fifth", 2400);
        ordinalMap.put("twenty-sixth", 2500);
        ordinalMap.put("twenty-seventh", 2600);
        ordinalMap.put("twenty-eighth", 2700);
        ordinalMap.put("twenty-ninth", 2800);
        ordinalMap.put("thirtieth", 2900);
        ordinalMap.put("1st", 1);
        ordinalMap.put("2nd", 100);
        ordinalMap.put("3rd", 200);
        ordinalMap.put("4th", 300);
        ordinalMap.put("5th", 400);
        ordinalMap.put("6th", 500);
        ordinalMap.put("7th", 600);
        ordinalMap.put("8th", 700);
        ordinalMap.put("9th", 800);
        ordinalMap.put("10th", 900);
        ordinalMap.put("11th", 1000);
        ordinalMap.put("12th", 1100);
        ordinalMap.put("13th", 1200);
        ordinalMap.put("14th", 1300);
        ordinalMap.put("15th", 1400);
        ordinalMap.put("16th", 1500);
        ordinalMap.put("17th", 1600);
        ordinalMap.put("18th", 1700);
        ordinalMap.put("19th", 1800);
        ordinalMap.put("20th", 1900);
        ordinalMap.put("21st", 2000);
        ordinalMap.put("22nd", 2100);
        ordinalMap.put("23rd", 2200);
        ordinalMap.put("24th", 2300);
        ordinalMap.put("25th", 2400);
        ordinalMap.put("26th", 2500);
        ordinalMap.put("27th", 2600);
        ordinalMap.put("28th", 2700);
        ordinalMap.put("29th", 2800);
        ordinalMap.put("30th", 2900);
        ordinals.addAll(ordinalMap.keySet());
    }

    @Getter(lazy=true) private static final String matchGroup = initMatchGroup();
    private static String initMatchGroup() {
        final StringBuilder b = new StringBuilder();
        for (String ordinal : ordinalMap.keySet()) {
            if (b.length() > 0) b.append("|");
            b.append(ordinal);
        }
        return "(" + b.toString() + ")";
    }

    public static long getYear (String ordinal) { return ordinalMap.get(ordinal); }

    public static String forYear(long year) {
        int index = (int) year/100;
        if (index >= ordinals.size()) die("forYear: error with "+year);
        return ordinals.get(index);
    }
}
