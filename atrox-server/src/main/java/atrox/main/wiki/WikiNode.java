package atrox.main.wiki;

import atrox.antlr.wiki.WikiArticleLexer;
import atrox.antlr.wiki.WikiArticleParser;
import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

public class WikiNode {

    public static List<WikiNode> parse(String text) {
        final WikiArticleLexer lexer = new WikiArticleLexer(new ANTLRInputStream(text));
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final WikiArticleParser parser = new WikiArticleParser(tokens);

        final WikiArticleParser.ArticleContext context = parser.article();
        final ParseTreeWalker walker = new ParseTreeWalker();
        final AntlrArticleListener listener = new AntlrArticleListener();
        walker.walk(listener, context);

        return listener.getNodes();
    }

    @Getter @Setter private WikiNodeType type;
    @Getter @Setter private String name;
    @Getter @Setter private List<WikiNode> children;

    public WikiNode(WikiNodeType type, String name) {
        setType(type);
        setName(name);
    }

    public void addNode (WikiNode node) {
        if (children == null) children = new ArrayList<>();
        children.add(node);
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
}
