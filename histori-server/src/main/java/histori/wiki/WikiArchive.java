package histori.wiki;

import cloudos.service.asset.AssetStorageService;
import cloudos.service.asset.AssetStream;
import cloudos.service.asset.S3AssetStorageService;
import com.amazonaws.util.StringInputStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.security.ShaUtil;

import java.util.Map;

import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;

@AllArgsConstructor @Slf4j
public class WikiArchive {

    private final AssetStorageService storage;

    public static final String[] SKIP_INDEX_PREFIXES = { "Category:", "Template:", "File:", "Template:" };

    public WikiArchive(Map<String, String> config) {
        this.storage = new S3AssetStorageService(config);
    }

    public void store(WikiArticle article) throws Exception {
        final String articlePath = getArticlePath(article.getTitle());
        if (storage.exists(articlePath)) {
            log.info("path already exists: "+articlePath);
            return;
        }
        storage.store(new StringInputStream(toJsonOrDie(article)), articlePath);
    }

    public ParsedWikiArticle find (String title) {
        AssetStream stream = null;
        try {
            stream = storage.load(getArticlePath(title));
            if (stream == null) return null;
            return fromJson(stream.getStream(), WikiArticle.class).parse();

        } catch (Exception e) {
            log.warn("find: "+e);

        } finally {
            if (stream != null) try { stream.close(); } catch (Exception ignored) {}
        }
        return null;
    }

    private static boolean isIndexable(String title) {
        title = title.toLowerCase().trim();
        for (String skip : SKIP_INDEX_PREFIXES) if (title.startsWith(skip)) return false;
        return true;
    }

    private static String getArticlePath(String title) {
        if (!isIndexable(title)) return null;
        final String sha256 = ShaUtil.sha256_hex(title);
        return sha256.substring(0, 2)
                + "/" + sha256.substring(2, 4)
                + "/" + sha256.substring(4, 6)
                + "/" + canonicalize(title) + "_" + sha256 + ".json";
    }
}
