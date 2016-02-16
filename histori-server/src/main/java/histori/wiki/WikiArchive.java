package histori.wiki;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.FileSuffixFilter;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.json.JsonUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;

@AllArgsConstructor @Slf4j
public class WikiArchive {

    @Getter @Setter private File baseDir;

    @Getter(lazy=true) private final Map<String, File> index = initIndex();

    private Map<String, File> initIndex () {
        final Map<String, File> map = new ConcurrentHashMap<>(10_000_000);
        for (File i : getIndexFiles(baseDir)) {
            log.info("initIndex: processing "+abs(i));
            for (String title : fromJsonOrDie(FileUtil.toStringOrDie(i), String[].class)) {
                if (isIndexable(title)) map.put(title, i);
            }
        }
        log.info("initIndex: completed, indexed "+map.size()+" articles");
        return map;
    }

    public static final String[] SKIP_INDEX_PREFIXES = { "Category:", "Template:", "File:", "Template:" };

    private boolean isIndexable(String title) {
        title = title.toLowerCase().trim();
        for (String skip : SKIP_INDEX_PREFIXES) if (title.startsWith(skip)) return false;
        return true;
    }

    public static File[] getIndexFiles(File wikiDir) {
        return FileUtil.listFiles(wikiDir, new FileSuffixFilter("_index.json"));
    }

    public ParsedWikiArticle find (String title) {
        final File articleBatch = getIndex().get(title);
        try {
            return articleBatch == null ? null : findArticle(title, articleBatch);
        } catch (Exception e) {
            log.error("Error finding article ("+title+"): "+e, e);
            return null;
        }
    }

    private ParsedWikiArticle findArticle(String title, File articleBatch) throws Exception {
        WikiJsonParseState parseState = WikiJsonParseState.seeking;

        try (InputStream in = new FileInputStream(articleBatch)) {
            try (GZIPInputStream zin = new GZIPInputStream(in)) {
                final JsonParser jp = JsonUtil.FULL_MAPPER.getFactory().createParser(zin);
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
                                if (title.equalsIgnoreCase(jp.getValueAsString())) {
                                    parseState = WikiJsonParseState.capture_text;
                                } else {
                                    parseState = WikiJsonParseState.seeking;
                                }
                            }
                            continue;

                        case capture_text:
                            if (jsonToken == JsonToken.VALUE_STRING) {
                                return new WikiArticle(title, jp.getValueAsString()).parse();
                            }
                            continue;
                    }
                }
            }
        }
        return null;
    }

}
