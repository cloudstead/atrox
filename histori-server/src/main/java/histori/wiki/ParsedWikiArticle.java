package histori.wiki;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

import static org.cobbzilla.util.string.StringUtil.isPunctuation;

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

    @Override public String toMarkdown() { return toMarkdown(null); }

    public String toMarkdown(Integer maxLen) {
        final StringBuilder b = new StringBuilder();
        boolean more = false;
        if (hasChildren()) {
            for (WikiNode child : getChildren()) {
                final String md = child.toMarkdown();
                if (md.length() == 0) continue;
                if (b.length() > 0 && !isPunctuation(md.charAt(0))) b.append(" ");
                b.append(md);
                if (maxLen != null && b.length() > maxLen) {
                    more = true;
                    break;
                }
            }
        }
        if (more) b.append(" ... ").append(newLinkNode(getName(), "continue reading").toMarkdown());

        String text = b.toString().replaceAll("\\(\\s*\\)", "");
        while (text.length() > 1 && isPunctuation(text.charAt(0))) text = text.substring(1);
        return text;
    }
}
