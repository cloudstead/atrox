package histori.wiki.matcher;

import histori.wiki.WikiNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
public class InfoboxNameMatcher extends InfoboxMatcher{

    @Getter @Setter private String name;

    @Override protected boolean infoboxMatches(WikiNode node) {
        return node.getName().toLowerCase().trim().equalsIgnoreCase(name);
    }

}
