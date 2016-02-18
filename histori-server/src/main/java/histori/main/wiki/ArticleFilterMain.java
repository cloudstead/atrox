package histori.main.wiki;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import histori.wiki.WikiArchive;
import histori.wiki.WikiArticle;
import histori.wiki.WikiJsonParseState;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.main.MainBase;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;

public class ArticleFilterMain extends MainBase<ArticleFilterOptions> {

    public static void main(String[] args) { main(ArticleFilterMain.class, args); }

    @Override protected void run() throws Exception {

        final ArticleFilterOptions options = getOptions();
        final JsonFactory jsonFactory = new JsonFactory();

        final File wikiDir = options.getDir();
        final File[] indexFiles = WikiArchive.getIndexFiles(wikiDir);
        for (int i=0; i<indexFiles.length; i++) {
            final File index = indexFiles[i];

            final Set<String> matchedTitles = new HashSet<>();
            for (String articleName : fromJson(index, String[].class)) {
                if (options.isFilterMatch(articleName)) matchedTitles.add(articleName);
            }

            final File xmlArticles = new File(index.getParentFile(), index.getName().replace("_index", "")+".gz");

            WikiJsonParseState parseState = WikiJsonParseState.seeking;
            WikiArticle article = new WikiArticle();
            int writeCount = 0;

            try (InputStream in = new FileInputStream(xmlArticles)) {
                try (GZIPInputStream zin = new GZIPInputStream(in)) {
                    final JsonParser jp = jsonFactory.createParser(zin);
                    jp.setCodec(JsonUtil.FULL_MAPPER);
                    JsonToken jsonToken;
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
                                    if (matchedTitles.contains(title)) {
                                        article.setTitle(title);
                                        parseState = WikiJsonParseState.capture_text;
                                    } else {
                                        parseState = WikiJsonParseState.seeking;
                                    }
                                }
                                continue;

                            case capture_text:
                                if (jsonToken == JsonToken.VALUE_STRING) {
                                    article.setText(jp.getValueAsString());
                                    final String title = article.getTitle();
                                    final String articleFileName = "match_" + canonicalize(title) + "_" + sha256_hex(title) + ".json";
                                    final File articleFile = new File(options.getOutputDir(), articleFileName);
                                    FileUtil.toFile(articleFile, toJson(article));
                                    writeCount++;
                                    parseState = WikiJsonParseState.seeking;
                                    article = new WikiArticle();
                                    out("wrote "+writeCount+"/"+matchedTitles.size()+" from "+xmlArticles.getName()+" in file "+(i+1)+"/"+indexFiles.length+": "+title);
                                }
                                continue;
                        }
                    }
                }
            }
        }
    }

}
