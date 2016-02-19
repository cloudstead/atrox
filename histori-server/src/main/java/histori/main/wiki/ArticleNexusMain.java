package histori.main.wiki;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import histori.model.NexusTag;
import histori.model.support.LatLon;
import histori.model.support.NexusRequest;
import histori.model.support.TimeRange;
import histori.wiki.ParsedWikiArticle;
import histori.wiki.WikiArchive;
import histori.wiki.WikiArticle;
import histori.wiki.WikiJsonParseState;
import histori.wiki.finder.DateRangeFinder;
import histori.wiki.finder.LocationFinder;
import histori.wiki.finder.TagFinder;
import histori.wiki.finder.TagFinderFactory;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.FileSuffixFilter;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.main.MainBase;
import org.geojson.Point;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.daemon.ZillaRuntime.uuid;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.listFiles;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.string.StringUtil.urlEncode;

@Slf4j
public class ArticleNexusMain extends MainBase<ArticleNexusOptions> {

    public static void main(String[] args) { main(ArticleNexusMain.class, args); }

    @Override protected void run() throws Exception {

        final ArticleNexusOptions options = getOptions();
        final File file = options.getFile();
        final File outputDir = options.getOutputDir();
        if (outputDir != null && !outputDir.isDirectory()) die("Output directory does not exist or is not a directory: "+abs(outputDir));

        WikiArticle article;

        if (file.isDirectory()) {
            // import all json files in directory, if they are valid WikiArticle json files
            for (File articleJson : listFiles(file, new FileSuffixFilter(".json"))) {
                try {
                    article = fromJson(FileUtil.toString(articleJson), WikiArticle.class);
                    importArticle(article);

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
                                        importArticle(article);
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
            importArticle(article);
        }
    }

    protected void importArticle (WikiArticle article) {

        if (article.getText().toLowerCase().startsWith("#redirect")) {
            err("importArticle: "+article.getTitle()+ " is a redirect (skipping)");
            return;
        }

        final ArticleNexusOptions options = getOptions();
        final File outputDir = options.getOutputDir();

        final ParsedWikiArticle parsed = article.parse();
        final WikiArchive wiki = options.getWikiArchive();
        final TimeRange dateRange;
        final LatLon coordinates;

        try {
            // When was it?
            dateRange = new DateRangeFinder().setWiki(wiki).setArticle(parsed).find();
            if (dateRange == null) {
                err("importArticle: "+article.getTitle()+ " had no date (skipping)");
                return;
            }

            // Where was it?
            coordinates = new LocationFinder().setWiki(wiki).setArticle(parsed).find();
            if (coordinates == null) {
                err("importArticle: "+article.getTitle()+ " had no coordinates (skipping)");
                return;
            }

            NexusRequest nexusRequest = (NexusRequest) new NexusRequest()
                    .setPoint(new Point(coordinates.getLon(), coordinates.getLat()))
                    .setTimeRange(dateRange)
                    .setName(parsed.getName());

            // extract additional tags
            final TagFinder tagFinder = TagFinderFactory.build(parsed);
            if (tagFinder != null) {
                final List<NexusTag> tags = tagFinder.find();
                nexusRequest.setTags(tags);
                nexusRequest.addTag("https://en.wikipedia.org/wiki/"+urlEncode(nexusRequest.getName()), "citation");
            }

            final String nexusJson = toJson(nexusRequest);
            if (outputDir != null) {
                final File out = new File(outputDir, canonicalize(nexusRequest.getName() + "_" + uuid() + ".json"));
                FileUtil.toFile(out, nexusJson);
            } else {
                out("\n----------\n" + nexusJson);
            }

        } catch (Exception e) {
            err("Error processing article: "+parsed.getName()+": "+e);
        }
    }

}
