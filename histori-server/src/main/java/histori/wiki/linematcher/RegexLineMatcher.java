package histori.wiki.linematcher;

import lombok.Getter;
import lombok.Setter;

import java.util.regex.Pattern;

public class RegexLineMatcher implements LineMatcher {

    @Getter @Setter private Pattern pattern;

    @Override public void configure(String args) { pattern = Pattern.compile(args); }

    @Override public boolean matches(String line) { return pattern.matcher(line).find(); }

}
