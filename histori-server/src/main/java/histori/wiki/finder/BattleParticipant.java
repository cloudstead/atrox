package histori.wiki.finder;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.StringTokenizer;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain = true) @EqualsAndHashCode(of = {"name", "side"})
class BattleParticipant {

    public String name;
    public String side;

    public BattleParticipant(String name) { this.name = name; }

    public boolean isValidName() {
        // must have at least 3 word chars
        return name.replaceAll("\\W", "").toLowerCase().trim().length() > BattleTagFinder.MIN_VALID_NAME_LENGTH;
    }

    public static BattleParticipant commander(String name, String side) {
        int pos = name.indexOf(',');
        if (pos == -1) pos = name.indexOf('.');
        if (pos != -1 && pos != name.length()-1) {
            int numWordsAfterComma = new StringTokenizer(name.substring(pos+1)).countTokens();
            if (numWordsAfterComma > 1) name = name.substring(0, pos);
        }
        return new BattleParticipant(name.trim(), side == null ? null : side.trim());
    }
}
