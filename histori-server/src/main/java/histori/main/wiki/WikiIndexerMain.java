package histori.main.wiki;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.cobbzilla.wizard.main.MainBase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static histori.main.wiki.WikiIndexerOptions.*;
import static org.cobbzilla.util.daemon.ZillaRuntime.now;
import static org.cobbzilla.util.system.Sleep.sleep;
import static org.cobbzilla.util.time.TimeUtil.formatDuration;

/**
 * Split a massive Wikipedia dump into one file per article.
 *
 * bzcat /path/to/enwiki-YYYYMMDD-pages-articles-multistream.xml.bz2 | ./run.sh index -w /path/to/index/basedir
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
 *   Use an ext4 filesystem with 300GB+ space and 40M+ inodes
 *   Mount with 'noatime'
 *   Any other best-practices for filesystems with a large number of small files
 *
 * DO NOT create the index on a filesystem with less than 300GB of space or 100M inodes.
 * Otherwise, you may run out of space or inodes before indexing completes.
 *
 * "Spot" extracting individual articles
 *
 * While time consuming, sometimes a linear scan of the entire archive is what is required. You can limit the
 * articles that are dumped using the -A or --articles command option. You can pass a file name, which is expected to
 * contain article titles, one per line.
 */
@Slf4j
public class WikiIndexerMain extends MainBase<WikiIndexerOptions> {

    public static final long STATUS_INTERVAL = TimeUnit.SECONDS.toMillis(30);

    public static void main (String[] args) { main(WikiIndexerMain.class, args); }

    @Getter protected AtomicInteger pageCount = new AtomicInteger(0);
    @Getter protected AtomicInteger storeCount = new AtomicInteger(0);

    private List<WikiIndexerTask> tasks;
    private List<Future> futures;

    private long lastStatus;
    private long startTime;

    @Override protected void run() throws Exception {

        final WikiIndexerOptions opts = getOptions();

        final int numThreads = opts.getThreads();
        if (numThreads > 1 && !opts.hasInfile()) die(OPT_THREADS+"/"+LONGOPT_THREADS+" was > 1 but "+OPT_INFILE+"/"+LONGOPT_INFILE+" was not set");
        if (numThreads > 1 && opts.hasSkip()) die(OPT_THREADS+"/"+LONGOPT_THREADS+" was > 1 and "+OPT_SKIP+"/"+LONGOPT_SKIP+" was set, can't do that");

        tasks = new ArrayList<>();
        futures = new ArrayList<>();

        startTime = now();
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for (int i=0; i<numThreads; i++) {
            final WikiIndexerTask task = new WikiIndexerTask(this, i);
            tasks.add(task);
            futures.add(executor.submit(task));
        }

        lastStatus = now();
        while (!futures.isEmpty()) {
            for (Iterator<Future> iter = futures.iterator(); iter.hasNext(); ) {
                final Future future = iter.next();
                try {
                    future.get(100, TimeUnit.MILLISECONDS);
                    iter.remove();
                } catch (TimeoutException ignored) {
                    // it's ok
                } catch (Exception e) {
                    // it's not ok
                    err("WikiIndexerTask ended with an error: "+e+"\n"+ ExceptionUtils.getStackTrace(e));
                    iter.remove();
                }
            }
            sleep(TimeUnit.SECONDS.toMillis(10));
            if (now() - lastStatus > STATUS_INTERVAL) printStatus();
        }

        printStatus();
        out("Wiki Index Completed! now="+now());
    }

    private void printStatus() {
        WikiIndexerOptions opts = getOptions();
        out(futures.size()+" index jobs running, "+pageCount.get()+" pages processed, "+storeCount.get()+" stored, duration: "+formatDuration(now() - startTime));
        if (opts.hasInfile()) {
            for (WikiIndexerTask task : tasks) {
                out("slice " + task.getSliceNumber() + ": " + task.getPercentDone() + " % complete");
            }
        }
        lastStatus = now();
    }

}
