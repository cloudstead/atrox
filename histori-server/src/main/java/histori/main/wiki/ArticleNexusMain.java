package histori.main.wiki;

import histori.model.support.NexusRequest;
import histori.wiki.WikiArticle;
import lombok.extern.slf4j.Slf4j;
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
import static org.cobbzilla.util.io.FileUtil.listFiles;
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
                    status("Error processing: "+line+": "+e);
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
            writeNexus(article);
            return;
        }

        if (file.isDirectory()) {
            // import all json files in directory, if they are valid WikiArticle json files
            for (File articleJson : listFiles(file, new FileSuffixFilter(".json"))) {
                article = fromJson(FileUtil.toString(articleJson), WikiArticle.class);
                writeNexus(article);
            }
        } else {
            // import a single file
            article = fromJson(FileUtil.toString(file), WikiArticle.class);
            writeNexus(article);
        }
    }

    private boolean writeNexus(WikiArticle article) {

        final ArticleNexusOptions options = getOptions();
        final File outputFile = getOutputFile(article.getTitle());
        if (!options.isOverwrite() && outputFile.exists()) {
            status("writeNexus: article file exists, not overwriting (" + outputFile + ")");
            return false;
        }

        final NexusRequest nexusRequest = options.getWikiArchive().toNexusRequest(article, disposition);
        if (nexusRequest == null) {
            status("writeNexus: Error building NexusRequest");
            return false;
        }

        final File outputDir = options.getOutputDir();
        if (outputDir != null && !outputDir.isDirectory()) die("Output directory does not exist or is not a directory: "+abs(outputDir));

        String title = nexusRequest.getName();
        try {
            final String nexusJson = toJson(nexusRequest);
            if (outputDir != null) {
                final File out = getOutputFile(title);
                final File parent = out.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) die("Error creating parent dir: "+abs(parent));
                FileUtil.toFile(out, nexusJson);
                out("\nWROTE: "+abs(out));
            } else {
                out("\n----------\n" + nexusJson);
            }
            status("SUCCESS");
            return true;

        } catch (Exception e) {
            status("Error processing article: "+ title +": "+e);
            return false;
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
