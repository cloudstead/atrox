package histori.wiki;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ParsedWikiArticle extends WikiNode {

    public ParsedWikiArticle (String title, List<WikiNode> nodes) {
        super(WikiNodeType.article, title);
        this.children = nodes;
    }

    @Getter @Setter private WikiNode activeNode;

    public WikiNode setActiveNode(WikiNodeType type, String name) {
        activeNode = findFirstWithName(type, name);
        return activeNode;
    }

}
