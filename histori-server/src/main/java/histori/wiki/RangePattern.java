package histori.wiki;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RangePattern {

    @Getter private String format;
    @Getter private String[] fields;

    @Getter @Setter private String[] eras = new String[]{""};

    @Getter(lazy=true) private final List<Pattern> patterns = initPatterns();
    private List<Pattern> initPatterns() {
        final List<Pattern> list = new ArrayList<>(eras.length);
        for (String suffix : eras) {
            if (suffix.length() > 0) suffix = " " + suffix;
            list.add(Pattern.compile(format+suffix, Pattern.CASE_INSENSITIVE));
        }
        return list;
    }

    public int getNumFields () { return fields.length; }
    public String getField (int i) { return fields[i]; }

    public RangePattern(String format, String... fields) {
        this.format = format;
        this.fields = fields;
    }

    public Matcher matches(String date) {
        for (Pattern p : getPatterns()) {
            final Matcher m = p.matcher(date);
            if (m.matches()) return m;
        }
        return null;
    }

    public Matcher find(String date) {
        for (Pattern p : getPatterns()) {
            final Matcher m = p.matcher(date);
            if (m.find()) return m;
        }
        return null;
    }
}
