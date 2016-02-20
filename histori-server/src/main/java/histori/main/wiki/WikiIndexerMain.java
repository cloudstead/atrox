package histori.main.wiki;

import histori.wiki.WikiArchive;
import histori.wiki.WikiArticle;
import histori.wiki.WikiXmlParseState;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.main.MainBase;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Split a massive Wikipedia dump into one file per article.
 *
 * bzcat /path/to/enwiki-YYYYMMDD-pages-articles-multistream.xml.bz2 | ./run.sh index -o /path/to/index/basedir
 *
 * You can also uncompress the archive and use 'cat' instead of 'bzcat'. This will be a bit faster than indexing
 * the bzipp'ed archive.
 *
 * The individual articles are stored in a directory structure where the path is determined by
 * the SHA-256 of the title, in the form /ab/cd/ef/first_100_chars_of_canonical_name_SHA256.json
 *
 * Where ab is the first 2 chars of the SHA, cd is the second 2 chars, and ef is the third 2 chars.
 *
 * This permits a directory structure with up to 256 subdirectories per level, and a total of 16M nested directories.
 * This directory structure allows near-instant lookup of any article based on its title, and avoids the problem of
 * having any single directory with thousands of files in it, which adversely affects filesystem performance.
 *
 * Stats:
 *   There are approximately 16M articles in the 13GB full Wikipedia archive.
 *   Uncompressed, the 13GB archive becomes about 56GB in size.
 *   On an EC2 m3.medium node, it takes about 24 hours to index the entire archive.
 *
 * Recommendations:
 *   When preparing a filesystem to write the index, some safe configuration parameters are:
 *   Use an ext4 filesystem, with 350GB+ space and 80M+ inodes.
 *   Mount with 'noatime'
 *
 * DO NOT create the index on a filesystem with less than 300GB of space or 100M inodes.
 * Otherwise, you will run out of space or inodes before indexing completes.
 */
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
