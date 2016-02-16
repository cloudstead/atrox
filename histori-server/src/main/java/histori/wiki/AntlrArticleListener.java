package histori.wiki;

import histori.antlr.wiki.WikiArticleParser;
import histori.antlr.wiki.WikiArticleParserBaseListener;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;

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

    @Override public void exitInfobox(WikiArticleParser.InfoboxContext ctx) { pop(); }

    @Override public void exitAttr(WikiArticleParser.AttrContext ctx) { pop(); }
    @Override public void exitLink(WikiArticleParser.LinkContext ctx) { pop(); }
    @Override public void enterAttrName(WikiArticleParser.AttrNameContext ctx) {
        final WikiNode node = new WikiNode(WikiNodeType.attribute, text(ctx));
        stack.push(node);
    }

    @Override public void enterInfoboxName(WikiArticleParser.InfoboxNameContext ctx) {
        final WikiNode node = new WikiNode(WikiNodeType.infobox, text(ctx));
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
