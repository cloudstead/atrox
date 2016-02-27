package histori.wiki.finder.impl;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.StringTokenizer;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @EqualsAndHashCode(of = {"name", "side"})
public class ConflictParticipant {

    public String name;
    public String side;

    public ConflictParticipant(String name) { this.name = name; }

    public boolean isValidName() {
        // must have at least 3 word chars
        return name.replaceAll("\\W", "").toLowerCase().trim().length() > ConflictFinder.MIN_VALID_NAME_LENGTH;
    }

    public static ConflictParticipant commander(String name, String side) {
        int pos = name.indexOf(',');
        if (pos == -1) pos = name.indexOf('.');
        if (pos != -1 && pos != name.length()-1) {
            int numWordsAfterComma = new StringTokenizer(name.substring(pos+1)).countTokens();
            if (numWordsAfterComma > 1) name = name.substring(0, pos);
        }
        if (name.endsWith("*")) name = name.substring(0, name.length()-1);
        return new ConflictParticipant(name.trim(), side == null ? null : side.trim());
    }
}
