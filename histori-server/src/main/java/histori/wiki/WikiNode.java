package histori.wiki;

import histori.antlr.wiki.WikiArticleLexer;
import histori.antlr.wiki.WikiArticleParser;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@Slf4j
public class WikiNode {

    public static WikiArticleParser.ArticleContext getArticleContext(String text) {
        // remove all HTML comments
        text = text.replaceAll("<!--.*?-->", "");
        final WikiArticleLexer lexer = new WikiArticleLexer(new ANTLRInputStream(text));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final WikiArticleParser parser = new WikiArticleParser(tokens);
        return parser.article();
    }

    public static List<WikiNode> parse(String text) {
        final WikiArticleParser.ArticleContext context = getArticleContext(text);
        final ParseTreeWalker walker = new ParseTreeWalker();
        final AntlrArticleListener listener = new AntlrArticleListener();
        walker.walk(listener, context);
        return listener.getNodes();
    }

    @Getter @Setter protected WikiNodeType type;
    @Getter @Setter protected String name;

    @Getter @Setter protected List<WikiNode> children;

    public boolean hasChildren() { return !empty(children); }

    @Getter(lazy=true) private final List<WikiNode> links = initLinks();
    private List<WikiNode> initLinks() { return findByType(WikiNodeType.link); }

    @Getter(lazy=true) private final List<WikiNode> infoboxes = initInfoboxes();
    private List<WikiNode> initInfoboxes() { return findByType(WikiNodeType.infobox); }

    public List<WikiNode> findByType(WikiNodeType type) {
        final List<WikiNode> links = new ArrayList<>();
        if (hasChildren()) for (WikiNode n : getChildren()) if (n.getType() == type) links.add(n);
        return links;
    }

    public WikiNode(WikiNodeType type, String name) {
        setType(type);
        setName(name);
    }

    public void addNode (WikiNode node) {
        if (children == null) children = new ArrayList<>();
        children.add(node);
    }

    public void addNodes (Collection<WikiNode> nodes) {
        if (children == null) children = new ArrayList<>();
        children.addAll(nodes);
    }

    @Override public String toString() { return toString(0); }

    private String toString(int indent) {
        final StringBuilder b = new StringBuilder("{"+type+"("+name+"): ");
        if (empty(children)) {
            b.append("<no-children> }");
        } else {
            indent += 4;
            for (WikiNode child : children) {
                b.append("\n");
                for (int i=0; i<indent; i++) b.append(' ');
                b.append(child.toString(indent));
            }
            b.append("\n");
            for (int i=0; i<indent-4; i++) b.append(' ');
            b.append("}");
        }

        return b.toString();

    }

    public String firstChildName () { return hasChildren() ? getChildren().get(0).getName() : null; }

    public WikiNode findFirstInfoboxWithName(String name) { return findFirstWithName(WikiNodeType.infobox, name); }

    public WikiNode findFirstAttributeWithName(String name) { return findFirstWithName(WikiNodeType.attribute, name); }

    public String findFirstAttributeValueWithName(String name) {
        final WikiNode attr = findFirstAttributeWithName(name);
        return attr != null ? attr.findAllChildText() : (String) die("getFirstAttributeValueWithName: attribute not found: " + name);
    }

    public WikiNode findFirstWithName(WikiNodeType type, String name) { return findFirstWithName(type, name, this); }

    public WikiNode findFirstWithName(WikiNodeType type, String name, WikiNode node) {
        if (node.getType() == type && node.getName().equalsIgnoreCase(name)) return node;
        if (node.hasChildren()) {
            for (WikiNode child : node.getChildren()) {
                final WikiNode found = findFirstWithName(type, name, child);
                if (found != null) return found;
            }
        }
        return null;
    }

    public WikiNode findFirstWithType(WikiNodeType type) { return findFirstWithType(type, this); }

    public WikiNode findFirstWithType(WikiNodeType type, WikiNode node) {
        if (node.hasChildren()) {
            for (WikiNode child : node.getChildren()) if (child.getType() == type) return child;
        }
        return null;
    }


    public String findAllChildText() {
        if (!hasChildren()) return null;
        final StringBuilder b = new StringBuilder();
        for (WikiNode n : getChildren()) {
            b.append(" ").append(n.findAllText());
        }
        return b.toString().trim();
    }

    public String findAllChildTextButNotLinkDescriptions() {
        if (!hasChildren()) return null;
        final StringBuilder b = new StringBuilder();
        for (WikiNode n : getChildren()) {
            if (n.getType() == WikiNodeType.link) {
                b.append(" ").append(n.getName());
            } else {
                b.append(" ").append(n.findAllText());
            }
        }
        return b.toString().trim();
    }

    public String findAllText() {
        final StringBuilder b = new StringBuilder(getName());
        if (hasChildren()) {
            for (WikiNode n : getChildren()) {
                b.append(" ").append(n.findAllText());
            }
        }
        return b.toString().trim();
    }

    public WikiNode findWithChildrenOrDie(String attrName) {
        final WikiNode n = findFirstAttributeWithName(attrName);
        if (n == null) return die("attribute "+attrName+" not found");
        return n.hasChildren() ? n : (WikiNode) die("attribute " + attrName + " had no value");
    }

    public WikiNode findChildNamed(String name) {
        if (!hasChildren()) return null;
        for (WikiNode n : getChildren()) if (n.getName().equalsIgnoreCase(name)) return n;
        return null;
    }

    public boolean hasChildNamed(String name) { return findChildNamed(name) != null; }

    public boolean hasSinglePlainlistChild() {
        if (!hasChildren()) return false;
        int listCount = 0;
        for (WikiNode child : getChildren()) if (child.getType().isPlainlist()) listCount++;
        return listCount == 1;
    }

    public WikiNode getParent(WikiNode node) {
        if (!hasChildren()) return null;
        for (WikiNode c : getChildren()) {
            if (c == node) return this;
            WikiNode found = c.getParent(node);
            if (found != null) return found;
        }
        return null;
    }
}
