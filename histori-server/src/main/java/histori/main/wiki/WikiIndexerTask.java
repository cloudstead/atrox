package histori.main.wiki;

import histori.wiki.WikiArchive;
import histori.wiki.WikiArticle;
import histori.wiki.WikiXmlParseState;
import histori.wiki.linematcher.LineMatcher;
import lombok.Getter;
import org.cobbzilla.util.io.ByteLimitedInputStream;
import org.cobbzilla.util.io.FileUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static histori.model.CanonicalEntity.canonicalize;
import static histori.wiki.WikiXmlParseState.*;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.system.Sleep.sleep;

class WikiIndexerTask implements Runnable {

    public static final String PAGE_TAG = "<page>";
    public static final String TITLE_TAG_OPEN = "<title>";
    public static final String TITLE_TAG_CLOSE = "</title>";
    public static final String TEXT_TAG_OPEN = "<text ";
    public static final String TEXT_TAG_CLOSE = "</text>";

    public static final int LINELOG_INTERVAL = 1_000_000;

    private final WikiIndexerMain main;
    @Getter private final int sliceNumber;

    public WikiIndexerTask(WikiIndexerMain main, int sliceNumber) {
        this.main = main;
        this.sliceNumber = sliceNumber;
    }

    private ByteLimitedInputStream inputStream;
    private int currentPage;

    public double getPercentDone () { return inputStream == null ? -1 : inputStream.getPercentDone(); }

    @Override public void run() {
        try { doRun(); } catch (Exception e) {
            die("run: " + e, e);
        }
    }

    private void doRun () throws Exception {
        final WikiIndexerOptions opts = main.getOptions();
        final WikiArchive wiki = opts.getWikiArchive();
        final AtomicInteger pageCount = main.getPageCount();

        Set<String> limitArticles = null;
        final File articleList = opts.getArticleList();
        if (articleList != null) {
            if (articleList.exists()) {
                limitArticles = new HashSet<>(FileUtil.toStringList(articleList));
            } else {
                die("Limit-article file does not exist: "+abs(articleList));
            }
        }

        final LineMatcher lineMatcher = opts.getLineMatcher();

        WikiXmlParseState parseState = seeking_page;
        WikiArticle article = new WikiArticle();

        final long startBytes = opts.getSkip(sliceNumber);
        final long endBytes = opts.getEnd(sliceNumber);
        inputStream = opts.getInputStream(startBytes, endBytes);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {

                if (++lineCount % LINELOG_INTERVAL == 0) {
                    main.out("processed line "+lineCount+" : "+ inputStream.getPercentDone()+" % complete");
                }

                line = line.trim();
                if (line.length() == 0) continue;

                switch (parseState) {
                    case seeking_page:
                        if (line.equals(PAGE_TAG)) {
                            currentPage = pageCount.incrementAndGet();
                            if (currentPage % 1000 == 0) main.out("handling page # "+ currentPage);
                            parseState = seeking_title;
                        }
                        continue;

                    case seeking_title:
                        if (line.startsWith(TITLE_TAG_OPEN) && line.endsWith(TITLE_TAG_CLOSE)) {
                            String title = line.replace(TITLE_TAG_OPEN, "").replace(TITLE_TAG_CLOSE, "");
                            if (limitArticles != null && !limitArticles.contains(title)) {
                                article = new WikiArticle();
                                parseState = seeking_page;
                                continue;
                            }
                            article.setTitle(title);
                            parseState = seeking_text;
                        }
                        continue;

                    case seeking_text:
                        if (line.startsWith(TEXT_TAG_OPEN)) {
                            if (!line.endsWith(TEXT_TAG_CLOSE)) {
                                if (lineMatcher != null && lineMatcher.matches(line)) {
                                    logMatch(article.getTitle());
                                    parseState = seeking_page;
                                    continue;
                                } else if (lineMatcher == null) {
                                    article.addText(line.substring(line.indexOf(">") + 1));
                                }
                                parseState = seeking_text_end;
                            } else {
                                // otherwise, this is a single-line entry
                                line = line.substring(line.indexOf(">") + 1);
                                line = line.substring(0, line.length() - TEXT_TAG_CLOSE.length());

                                if (lineMatcher != null && lineMatcher.matches(line)) {
                                    logMatch(article.getTitle());
                                    parseState = seeking_page;
                                    continue;

                                } else if (lineMatcher == null) {
                                    article.addText(line);
                                    store(wiki, article);
                                }
                                article = new WikiArticle();
                                parseState = seeking_page;
                            }
                        }
                        continue;

                    case seeking_text_end:
                        if (lineMatcher != null && lineMatcher.matches(line)) {
                            logMatch(article.getTitle());
                            parseState = seeking_page;
                            continue;

                        } else if (line.endsWith(TEXT_TAG_CLOSE)) {
                            if (lineMatcher == null) {
                                article.addText("\n" + line.substring(0, line.length() - TEXT_TAG_CLOSE.length()));
                                store(wiki, article);
                            }
                            article = new WikiArticle();
                            parseState = seeking_page;

                        } else if (lineMatcher == null) {
                            article.addText("\n"+line);
                        }
                        continue;

                    default:
                        die("Invalid state: "+parseState);
                }
            }
        }
    }

    private static final ConcurrentHashMap<String, Boolean> writing = new ConcurrentHashMap<>();

    private void store(final WikiArchive wiki, final WikiArticle article) {

        final String canonical = canonicalize(article.getTitle());
        try {
            Boolean sync;

            // All this elaborate code is to:
            //   1. avoid all the Task threads synchronizing on a single shard object for every write (would hurt performance)
            //   2. prevent multiple Tasks from updating the same nexus at the same time.
            // For example, one thread wants to write an alias, the other a full article. When we serialize access, we ensure a proper result.
            sync = writing.get(canonical);
            while (sync != null) {
                sleep(100);
                sync = writing.get(canonical);
            }
            boolean hasLock = true;
            synchronized (writing) {
                sync = writing.get(canonical);
                if (sync != null) {
                    hasLock = false;
                } else {
                    writing.put(canonical, true);
                }
            }
            if (!hasLock) {
                // try again on another call, a little later
                sleep(1000);
                store(wiki, article);
                return;
            }

            // OK, now we can write the file
            final WikiIndexerOptions options = main.getOptions();

            final boolean exists = wiki.exists(article);
            if (!options.isOverwrite() && exists) {
                // only skip overwriting if article is a real article. redirects will get overwritten (unless we ourselves are also a redirect)
                final WikiArticle existing = wiki.findUnparsed(article.getTitle());
                if (existing != null && (!existing.isRedirect() || article.isRedirect())) return;
            }

            // even if overwrite is enabled, never overwrite a regular article with a redirect article
            // this could sometimes happen if a redirect only differs in spelling from the main article
            if (options.isOverwrite() && exists) {
                final WikiArticle existing = wiki.findUnparsed(article.getTitle());
                if (existing == null) {
                    main.out("store: exists(" + article.getTitle() + ") return true, but findUnparsed return null: overwriting (unparseable JSON?)");

                } else if (!existing.isRedirect() && article.isRedirect()) {
                    // article is a redirect but existing one is not: do not overwrite
                    // out("store: not overwriting regular article ("+existing.getTitle()+") with redirect ("+article.getTitle()+")");
                    return;
                }
            }

            final String title = article.getTitle();
            try {
                if (wiki.store(article)) {
                    final int count = main.getStoreCount().incrementAndGet();
                    if (count % 1000 == 0) main.out("stored page # " + count + " (" + title + ")");
                }

            } catch (Exception e) {
                die("error storing: " + title + " (page " + currentPage + "): " + e, e);
            }

        } finally {
            // we're done, let other write to this article if they want
            writing.remove(canonical);
        }
    }

    private void logMatch(String title) {
        // ensure only one item is logged at a time
        synchronized (main) {
            final WikiIndexerOptions options = main.getOptions();
            if (options.hasFilterLog()) {
                FileUtil.toFileOrDie(options.getFilterLog(), title.trim() + "\n", true);
            } else {
                main.out("FILTER-MATCH: " + title);
            }
        }
    }

}
