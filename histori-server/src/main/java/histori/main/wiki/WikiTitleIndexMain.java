package histori.main.wiki;

import histori.wiki.WikiArchive;
import lombok.Cleanup;
import org.cobbzilla.wizard.main.MainBase;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import static org.cobbzilla.util.string.StringUtil.chop;

public class WikiTitleIndexMain extends MainBase<WikiTitleIndexOptions> {

    public static final String TITLE_OPEN = "<title>";
    public static final String TITLE_CLOSE = "</title>";

    public static final long LOG_INTERVAL = 100_000;

    public static void main (String[] args) { main(WikiTitleIndexMain.class, args); }

    @Override protected void run() throws Exception {
        final WikiTitleIndexOptions opts = getOptions();
        final boolean sort = opts.isSort();
        String[] indexLines = new String[0];
        if (sort) {
            final int sortSize = opts.getSortSize();
            err("allocating array of size "+sortSize+"...");
            indexLines = new String[sortSize];
            err("successfully allocated array of size "+sortSize);
        }

        String line;
        @Cleanup final BufferedReader reader = new BufferedReader(new InputStreamReader(opts.getStream()));
        long count = 0;
        int index = 0;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            int pos = line.indexOf(TITLE_OPEN);
            if (pos != -1) {
                line = chop(line.substring(pos + TITLE_OPEN.length()), TITLE_CLOSE).trim();
                final String path = WikiArchive.getArticlePath(line);
                if (path != null) {
                    if (sort) {
                        if (index >= indexLines.length) {
                            final String[] expanded = new String[indexLines.length + 10000];
                            System.arraycopy(indexLines, 0, expanded, 0, indexLines.length);
                            indexLines = expanded;
                        }
                        indexLines[index] = line+"\t"+path;
                        index++;
                    } else {
                        out(line+"\t"+path);
                    }
                    if (++count % LOG_INTERVAL == 0) err("processed title "+count);
                }
            }
        }
        if (sort) {
            for (int i=index+1; i<indexLines.length; i++) {
                // blank-pad remaining entries so string comparator doesn't barf with NPE
                indexLines[i] = "";
            }
            err("sorting "+indexLines.length+" titles...");
            Arrays.sort(indexLines, String.CASE_INSENSITIVE_ORDER);
            for (String indexLine : indexLines) {
                if (indexLine.length() > 0) out(indexLine);
            }
        }
    }
}
