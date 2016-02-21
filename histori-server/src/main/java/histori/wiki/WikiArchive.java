package histori.wiki;

import cloudos.service.asset.AssetStorageService;
import cloudos.service.asset.AssetStream;
import com.amazonaws.util.StringInputStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.security.ShaUtil;
import org.cobbzilla.util.string.StringUtil;

import static histori.model.CanonicalEntity.canonicalize;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;

@AllArgsConstructor @Slf4j
public class WikiArchive {

    private final AssetStorageService storage;

    public static final String[] SKIP_INDEX_PREFIXES = { "Category:", "Template:", "File:", "Template:" };

    public boolean exists (WikiArticle article) {
        final String articlePath = getArticlePath(article.getTitle());
        return articlePath != null && storage.exists(articlePath);
    }

    public void store(WikiArticle article) throws Exception {
        final String articlePath = getArticlePath(article.getTitle());
        if (articlePath == null) {
            log.info("refusing to index: "+article.getTitle());
            return;
        } else if (storage.exists(articlePath)) {
            log.info("path already exists: "+articlePath);
            return;
        }
        storage.store(new StringInputStream(toJsonOrDie(article)), articlePath, articlePath);
    }

    public ParsedWikiArticle find (String title) {
        final WikiArticle article = findUnparsed(title);
        return article != null ? article.parse() : null;
    }

    public WikiArticle findUnparsed (String title) {
        AssetStream stream = null;
        try {
            final String articlePath = getArticlePath(title);
            if (articlePath == null) return null;

            stream = storage.load(articlePath);
            if (stream == null) return null;
            return fromJson(stream.getStream(), WikiArticle.class);

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

    public static String getArticlePath(String title) {
        if (!isIndexable(title)) return null;
        final String sha256 = ShaUtil.sha256_hex(title);
        return sha256.substring(0, 2)
                + "/" + sha256.substring(2, 4)
                + "/" + sha256.substring(4, 6)
                + "/" + StringUtil.truncate(canonicalize(title), 100) + "_" + sha256 + ".json";
    }
}
