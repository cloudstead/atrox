package histori.main.wiki;

import histori.model.support.MultiNexusRequest;
import histori.model.support.NexusRequest;
import histori.wiki.WikiArticle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.cobbzilla.util.io.FileSuffixFilter;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.main.MainBase;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static histori.wiki.WikiArchive.getArticlePath;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.listFilesRecursively;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Slf4j
public class ArticleNexusMain extends MainBase<ArticleNexusOptions> {

    public static void main(String[] args) { main(ArticleNexusMain.class, args); }

    private List<String> disposition = null;

    @Override protected void run() throws Exception {

        final ArticleNexusOptions options = getOptions();

        String input = options.getInput();
        if (input == null) {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = r.readLine()) != null) {
                try {
                    nexus(line);
                } catch (Exception e) {
                    status("Error processing: "+line+": "+e+(e instanceof NullPointerException ? " ("+ ExceptionUtils.getStackTrace(e).replace("\n", " -- ") + ")" : ""));
                } finally {
                    logStatus(options, line);
                }
            }
        } else {
            try {
                nexus(input);
            } finally {
                logStatus(options, input);
            }
        }
    }

    private void logStatus(ArticleNexusOptions options, String line) {
        if (options.hasErrorLog()) FileUtil.toFileOrDie(options.getErrorLog(), "\n"+line+"\t: "+ dispositionString(), true);
    }

    private String dispositionString() { return StringUtil.toString(disposition, "|").replace("\n", "\\n"); }

    private void nexus(String input) throws Exception {

        disposition = new ArrayList<>();
        input = input.trim(); // no sane input would have leading/trailing whitespace, but a line from a file or stdin just might. play it safe.

        WikiArticle article;
        final ArticleNexusOptions options = getOptions();
        final File file = new File(input);

        if (!file.exists()) {
            // maybe it is an article name?
            article = options.getWikiArchive().findUnparsed(input);
            if (article == null) {
                status("article not found: "+input);
                return;
            }
            writeNexus(article);
            return;
        }

        if (file.isDirectory()) {
            // import all json files in directory, if they are valid WikiArticle json files
            for (File articleJson : listFilesRecursively(file, new FileSuffixFilter(".json"))) {
                disposition = new ArrayList<>();
                try {
                    article = fromJson(FileUtil.toString(articleJson), WikiArticle.class);
                    writeNexus(article);
                } catch (Exception e) {
                    log.error("error writing nexus: "+e);
                    logStatus(options, input);
                }
            }
        } else {
            // import a single file
            article = fromJson(FileUtil.toString(file), WikiArticle.class);
            writeNexus(article);
        }
    }

    /**
     * @param article the article to write nexus data about
     * @return number of nexus requests created
     */
    private int writeNexus(WikiArticle article) {

        final ArticleNexusOptions options = getOptions();
        final File outputFile = getOutputFile(article.getTitle());
        if (!options.isOverwrite() && outputFile.exists()) {
            status("writeNexus: article file exists, not overwriting (" + outputFile + ")");
            return 0;
        }

        final NexusRequest nexusRequest = options.getWikiArchive().toNexusRequest(article, disposition);
        if (nexusRequest == null) {
            status("writeNexus: Error building NexusRequest");
            return 0;
        }

        final File outputDir = options.getOutputDir();
        if (outputDir != null && !outputDir.isDirectory()) die("Output directory does not exist or is not a directory: "+abs(outputDir));

        if (nexusRequest instanceof MultiNexusRequest) {
            MultiNexusRequest multi = (MultiNexusRequest) nexusRequest;
            if (!multi.hasRequests()) return 0;
            int count = 0;
            for (NexusRequest request : multi.getRequests()) {
                count += writeSingleRequest(request, outputDir);
            }
            return count;

        } else {
            return writeSingleRequest(nexusRequest, outputDir);
        }
    }

    private int writeSingleRequest(NexusRequest nexusRequest, File outputDir) {
        String title = nexusRequest.getName();
        try {
            final String nexusJson = toJson(nexusRequest);
            if (outputDir != null) {
                final File out = getOutputFile(title);
                final File parent = out.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) die("Error creating parent dir: " + abs(parent));
                FileUtil.toFile(out, nexusJson);
                out("\nWROTE: " + abs(out));
            } else {
                out("\n----------\n" + nexusJson);
            }
            status("SUCCESS");
            return 1;

        } catch (Exception e) {
            status("Error processing article: " + title + ": " + e);
            return 0;
        }
    }

    private void status(String message) {
        if (disposition != null) disposition.add(message);
        if (!message.equals("SUCCESS")) err(message);
    }

    private File getOutputFile(String title) {
        final String path = getArticlePath(title, "nexus");
        if (path == null) die("Cannot save: "+ title);
        return new File(abs(getOptions().getOutputDir()) + "/" + path);
    }

}
