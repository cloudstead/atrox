package atrox.main.wiki;

import lombok.Getter;
import org.cobbzilla.util.string.StringUtil;

import java.util.ArrayList;
import java.util.List;

public class ParsedWikiArticle extends ArrayList<WikiNode> {

    @Getter private final String title;

    public ParsedWikiArticle (String title, List<WikiNode> nodes) {
        this.title = title;
        addAll(nodes);
    }

    @Override public String toString() {
        return "ParsedWikiArticle("+title+"):\n"+StringUtil.toString(this, "\n")+"\nEND ParsedWikiArticle("+title+")\n";
    }
}
