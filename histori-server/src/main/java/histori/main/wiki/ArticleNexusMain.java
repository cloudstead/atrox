package histori.main.wiki;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import histori.model.support.NexusRequest;
import histori.wiki.WikiArchive;
import histori.wiki.WikiArticle;
import histori.wiki.WikiJsonParseState;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.FileSuffixFilter;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.main.MainBase;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.listFiles;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;

@Slf4j
public class ArticleNexusMain extends MainBase<ArticleNexusOptions> {

    public static void main(String[] args) { main(ArticleNexusMain.class, args); }

    @Override protected void run() throws Exception {

        final ArticleNexusOptions options = getOptions();

        String input = options.getInput();
        if (input == null) {
            BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
            String line;
            while ((line = r.readLine()) != null) {
                nexus(line);
            }
        } else {
            nexus(input);
        }
    }

    private void nexus(String input) throws Exception {
        input = input.trim(); // no sane input would have leading/trailing whitespace, but a line from a file or stdin just might. play it safe.
        final ArticleNexusOptions options = getOptions();
        final File file = new File(input);
        if (!file.exists()) {
            // maybe it is an article name?
            WikiArticle article = options.getWikiArchive().findUnparsed(input);
            if (article == null) die("Article not found: "+input);
            writeNexus(article);
            return;
        }

        final File outputDir = options.getOutputDir();
        if (outputDir != null && !outputDir.isDirectory()) die("Output directory does not exist or is not a directory: "+abs(outputDir));

        WikiArticle article;

        if (file.isDirectory()) {
            // import all json files in directory, if they are valid WikiArticle json files
            for (File articleJson : listFiles(file, new FileSuffixFilter(".json"))) {
                try {
                    article = fromJson(FileUtil.toString(articleJson), WikiArticle.class);
                    writeNexus(article);

                } catch (Exception e) {
                    err("Error importing " + abs(articleJson) + ": " + e);
                }
            }

            // import all .gz files in directory, if they are valid Wiki split files
            WikiJsonParseState parseState = WikiJsonParseState.seeking;
            for (File wikiSplitFile : listFiles(file, new FileSuffixFilter(".json.gz"))) {
                try (FileInputStream fin = new FileInputStream(wikiSplitFile)) {
                    try (GZIPInputStream gzin = new GZIPInputStream(fin)) {
                        final JsonParser jp = JsonUtil.FULL_MAPPER.getFactory().createParser(gzin);
                        jp.setCodec(JsonUtil.FULL_MAPPER);
                        JsonToken jsonToken;
                        article = new WikiArticle();
                        while ((jsonToken = jp.nextToken()) != null) {
                            switch (parseState) {
                                case seeking:
                                    if (jsonToken == JsonToken.FIELD_NAME && jp.getValueAsString().equals("title")) {
                                        parseState = WikiJsonParseState.capture_title;
                                    }
                                    continue;

                                case capture_title:
                                    if (jsonToken == JsonToken.VALUE_STRING) {
                                        final String title = jp.getValueAsString();
                                        article.setTitle(title);
                                        parseState = WikiJsonParseState.capture_text;
                                    }
                                    continue;

                                case capture_text:
                                    if (jsonToken == JsonToken.VALUE_STRING) {
                                        article.setText(jp.getValueAsString());
                                        writeNexus(article);
                                        parseState = WikiJsonParseState.seeking;
                                        article = new WikiArticle();
                                    }
                                    continue;
                            }
                        }
                    }
                }
            }
        } else {
            // import a single file
            article = fromJson(FileUtil.toString(file), WikiArticle.class);
            writeNexus(article);
        }
    }

    private void writeNexus(WikiArticle article) {

        final ArticleNexusOptions options = getOptions();
        final NexusRequest nexusRequest = options.getWikiArchive().toNexusRequest(article);
        if (nexusRequest == null) {
            err("writeNexus: Error building NexusRequest");
            return;
        }

        final File outputDir = options.getOutputDir();

        try {
            final String nexusJson = toJson(nexusRequest);
            if (outputDir != null) {
                final String path = WikiArchive.getArticlePath(nexusRequest.getName(), "nexus");
                if (path == null) die("Cannot save: "+nexusRequest.getName());

                final File out = new File(abs(outputDir) + "/" + path);
                if (!out.getParentFile().mkdirs()) die("Error creating parent dir: "+abs(out.getParentFile()));
                FileUtil.toFile(out, nexusJson);
                out("WROTE: "+abs(out));
            } else {
                out("\n----------\n" + nexusJson);
            }

        } catch (Exception e) {
            err("Error processing article: "+nexusRequest.getName()+": "+e);
        }
    }

}
