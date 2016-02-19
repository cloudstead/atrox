package histori.main.wiki;

import histori.wiki.WikiArchive;
import histori.wiki.WikiArticle;
import histori.wiki.WikiXmlParseState;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.main.MainBase;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
public class WikiIndexerMain extends MainBase<WikiIndexerOptions> {

    public static final String PAGE_TAG = "<page>";
    public static final String TITLE_TAG_OPEN = "<title>";
    public static final String TITLE_TAG_CLOSE = "</title>";
    public static final String TEXT_TAG_OPEN = "<text ";
    public static final String TEXT_TAG_CLOSE = "</text>";

    public static void main (String[] args) { main(WikiIndexerMain.class, args); }

    private int pageCount = 0;
    private int storeCount = 0;

    @Override protected void run() throws Exception {

        final WikiIndexerOptions opts = getOptions();
        final WikiArchive wiki = opts.getWikiArchive();

        final int skipPages = opts.getSkipPages();

        WikiXmlParseState parseState = WikiXmlParseState.seeking_page;
        WikiArticle article = new WikiArticle();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) continue;
                switch (parseState) {
                    case seeking_page:
                        if (line.equals(PAGE_TAG)) {
                            if (++pageCount % 1000 == 0) out("handling page # "+pageCount);
                            if (pageCount > skipPages) {
                                parseState = WikiXmlParseState.seeking_title;
                            } else {
                                if (pageCount % 1000 == 0) out("skipped "+pageCount+" articles: "+System.currentTimeMillis());
                                continue;
                            }
                        }
                        continue;

                    case seeking_title:
                        if (line.startsWith(TITLE_TAG_OPEN) && line.endsWith(TITLE_TAG_CLOSE)) {
                            article.setTitle(line.replace(TITLE_TAG_OPEN, "").replace(TITLE_TAG_CLOSE, ""));
                            parseState = WikiXmlParseState.seeking_text;
                        }
                        continue;

                    case seeking_text:
                        if (line.startsWith(TEXT_TAG_OPEN)) {
                            article.addText(line.substring(line.indexOf(">")+1));
                            parseState = WikiXmlParseState.seeking_text_end;
                        }
                        continue;

                    case seeking_text_end:
                        if (line.endsWith(TEXT_TAG_CLOSE)) {
                            article.addText("\n"+line.substring(0, line.length() - TEXT_TAG_CLOSE.length()));

                            store(wiki, article);

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

    private void store(final WikiArchive wiki, final WikiArticle article) {

        if (wiki.exists(article)) return;

        try {
            wiki.store(article);
            if (++storeCount % 1000 == 0) out("stored page # "+storeCount);

        } catch (Exception e) {
            die("error storing: " + article.getTitle() + " (page " + pageCount + "): " + e, e);
        }
    }

}
