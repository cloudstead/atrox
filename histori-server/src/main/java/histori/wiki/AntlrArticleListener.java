package histori.wiki;

import histori.antlr.wiki.WikiArticleParser;
import histori.antlr.wiki.WikiArticleParserBaseListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@Slf4j
public class AntlrArticleListener extends WikiArticleParserBaseListener {

    private final Stack<WikiNode> stack = new Stack<>();
    @Getter private final List<WikiNode> nodes = new ArrayList<>();

    public void pop() {
        final WikiNode node = stack.pop();
        if (stack.isEmpty()) {
            nodes.add(node);
        } else {
            stack.peek().addNode(node);
        }
    }
    public String text(ParserRuleContext ctx) { return ctx.getText().trim(); }

    /**
     * Parse the body and append all nodes to node
     * @param node Parsed nodes will be appended to this node as children
     * @param body The wiki text to parse
     */
    private void parse(WikiNode node, String body) {
        final WikiArticleParser.ArticleContext context = WikiNode.getArticleContext(body);
        final ParseTreeWalker walker = new ParseTreeWalker();
        final AntlrArticleListener listener = new AntlrArticleListener();
        walker.walk(listener, context);
        node.addNodes(listener.getNodes());
    }

    @Override public void enterWikitable(WikiArticleParser.WikitableContext ctx) {
        final String raw = text(ctx);
        final WikiNode node = new WikiNode(WikiNodeType.wikitable, raw);
        if (stack.isEmpty()) {
            nodes.add(node);
        } else {
            stack.peek().addNode(node);
        }
        String inner;
        int nlPos = raw.indexOf('\n');
        inner = (nlPos != -1) ? raw.substring(nlPos).trim() : raw.trim();
        if (inner.startsWith("|") && inner.length() > 1) inner = inner.substring(1);

        int closePos = inner.lastIndexOf("|}");
        inner = closePos != -1 ? inner.substring(0, closePos) : inner;
        if (!inner.endsWith("<missing END_WIKITABLE>")) parse(node, inner);
    }

    @Override public void enterPlainlist(WikiArticleParser.PlainlistContext ctx) {
        final WikiNode node = new WikiNode(WikiNodeType.plainlist, text(ctx));
        stack.push(node);
    }

    @Override public void exitPlainlist(WikiArticleParser.PlainlistContext ctx) { pop(); }

    @Override public void enterPlainlistEntry(WikiArticleParser.PlainlistEntryContext ctx) {
        final String text = text(ctx);
        final WikiNode node;
        if (text.startsWith(";")) {
            final String body = text.substring(1).trim();
            node = new WikiNode(WikiNodeType.plainlist_header, body);
            parse(node, body);
        } else {
            final String body = (text.startsWith("*") || text.startsWith(":")) ? text.substring(1).trim() : text;
            node = new WikiNode(WikiNodeType.plainlist_entry, body);
            parse(node, body);
        }
        if (!stack.isEmpty()) stack.peek().addNode(node);
    }

    @Override public void enterInfoboxName(WikiArticleParser.InfoboxNameContext ctx) {
        final WikiNode node = new WikiNode(WikiNodeType.infobox, text(ctx));
//        if (!stack.isEmpty()) stack.peek().addNode(node);
        stack.push(node);
    }
    @Override public void exitInfobox(WikiArticleParser.InfoboxContext ctx) { pop(); }

    @Override public void enterAttrName(WikiArticleParser.AttrNameContext ctx) {
        final WikiNode node = new WikiNode(WikiNodeType.attribute, text(ctx));
        stack.push(node);
    }

    @Override public void enterAttrText(WikiArticleParser.AttrTextContext ctx) {
        stack.peek().addNode(new WikiNode(WikiNodeType.string, text(ctx)));
    }

    @Override public void exitAttr(WikiArticleParser.AttrContext ctx) { pop(); }

    @Override public void enterLinkTarget(WikiArticleParser.LinkTargetContext ctx) {
        final WikiNode node = new WikiNode(WikiNodeType.link, text(ctx));
        stack.push(node);
    }

    @Override public void enterLinkMetaString(WikiArticleParser.LinkMetaStringContext ctx) {
        final WikiNode node = new WikiNode(WikiNodeType.string, text(ctx));
        stack.peek().addNode(node);
    }

    @Override public void exitLink(WikiArticleParser.LinkContext ctx) { pop(); }

    @Override public void enterFreeform(WikiArticleParser.FreeformContext ctx) {
        final WikiNode node = new WikiNode(WikiNodeType.string, text(ctx));
        if (stack.isEmpty()) {
            nodes.add(node);
        } else {
            stack.peek().addNode(node);
        }
    }
}
