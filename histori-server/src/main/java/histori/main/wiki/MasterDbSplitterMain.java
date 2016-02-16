package histori.main.wiki;

import histori.wiki.WikiArticle;
import histori.wiki.WikiDbSplitter;
import histori.wiki.WikiXmlParseState;
import org.cobbzilla.wizard.main.MainBase;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class MasterDbSplitterMain extends MainBase<MasterDbSplitterOptions> {

    public static void main (String[] args) { main(MasterDbSplitterMain.class, args); }

    @Override protected void run() throws Exception {

        final MasterDbSplitterOptions opts = getOptions();

        final File outputDir = opts.getOutputDir();
        final String prefix = opts.getOutputPrefix();
        final int splitSize = opts.getSplitSize();
        final int fileNumber = opts.getFileNumber();
        final int skipPages = opts.getSkipPages();

        WikiXmlParseState parseState = WikiXmlParseState.seeking_page;
        WikiArticle article = new WikiArticle();
        int pageCount = 0;

        try (WikiDbSplitter wikiDbSplitter = new WikiDbSplitter(outputDir, prefix, splitSize, fileNumber)) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.length() == 0) continue;
                    switch (parseState) {
                        case seeking_page:
                            if (line.equals("<page>")) {
                                pageCount++;
                                if (pageCount > skipPages) {
                                    parseState = WikiXmlParseState.seeking_title;
                                } else {
                                    if (pageCount % 1000 == 0) out("skipped "+pageCount+" articles: "+System.currentTimeMillis());
                                    continue;
                                }
                            }
                            continue;

                        case seeking_title:
                            if (line.startsWith("<title>") && line.endsWith("</title>")) {
                                article.setTitle(line.replace("<title>", "").replace("</title>", ""));
                                parseState = WikiXmlParseState.seeking_text;
                            }
                            continue;

                        case seeking_text:
                            if (line.startsWith("<text ")) {
                                article.addText(line.substring(line.indexOf(">")+1));
                                parseState = WikiXmlParseState.seeking_text_end;
                            }
                            continue;

                        case seeking_text_end:
                            if (line.endsWith("</text>")) {
                                article.addText("\n"+line.substring(0, line.length()-"</text>".length()));
                                wikiDbSplitter.writeArticle(article);
                                article = new WikiArticle();
                                parseState = WikiXmlParseState.seeking_page;
                            } else {
                                article.addText("\n"+line);
                            }
                            continue;

                        default:
                            die("Invalid state: "+parseState);
                    }
                }
            }
        }
    }

}
