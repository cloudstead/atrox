package atrox.main.wiki;

import atrox.antlr.wiki.WikiArticleLexer;
import atrox.antlr.wiki.WikiArticleParser;
import atrox.antlr.wiki.WikiArticleParserBaseListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

@Slf4j
public class AntlrArticleListener extends WikiArticleParserBaseListener {

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

    @Override public void exitBlock(WikiArticleParser.BlockContext ctx) { pop(); }

    @Override public void exitAttr(WikiArticleParser.AttrContext ctx) { pop(); }
    @Override public void exitLink(WikiArticleParser.LinkContext ctx) { pop(); }
    @Override public void enterAttrName(WikiArticleParser.AttrNameContext ctx) {
        final WikiNode node = new WikiNode(WikiNodeType.attribute, text(ctx));
        stack.push(node);
    }

    @Override public void enterBlockName(WikiArticleParser.BlockNameContext ctx) {
        final WikiNode node = new WikiNode(WikiNodeType.block, text(ctx));
        if (!stack.isEmpty()) stack.peek().addNode(node);
        stack.push(node);
    }

    @Override public void enterAttrText(WikiArticleParser.AttrTextContext ctx) {
        stack.peek().addNode(new WikiNode(WikiNodeType.string, text(ctx)));
    }

    @Override public void enterLinkTarget(WikiArticleParser.LinkTargetContext ctx) {
        final WikiNode node = new WikiNode(WikiNodeType.link, text(ctx));
        stack.push(node);
    }

    @Override public void enterLinkMetaString(WikiArticleParser.LinkMetaStringContext ctx) {
        final WikiNode node = new WikiNode(WikiNodeType.string, text(ctx));
        stack.peek().addNode(node);
    }

    @Override public void enterFreeform(WikiArticleParser.FreeformContext ctx) {
        final WikiNode node = new WikiNode(WikiNodeType.string, text(ctx));
        if (stack.isEmpty()) {
            nodes.add(node);
        } else {
            stack.peek().addNode(node);
        }
    }
}
