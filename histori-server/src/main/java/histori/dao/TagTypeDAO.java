package histori.dao;

import histori.model.TagType;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;

@Repository
public class TagTypeDAO extends CanonicalEntityDAO<TagType> {

    // findByCanonicalName needs to be lightning-fast
    private static final long CACHE_REFRESH = TimeUnit.DAYS.toMillis(30);
    private final AtomicLong lastCacheFill = new AtomicLong(0);
    private final Map<String, TagType> tagTypeCache = new ConcurrentHashMap<>();

    @Override public TagType findByCanonicalName(String canonicalName) {
        if (now() - lastCacheFill.get() > CACHE_REFRESH) {
            synchronized (lastCacheFill) {
                if (now() - lastCacheFill.get() > CACHE_REFRESH) {
                    for (TagType tagType : findAll()) {
                        tagTypeCache.put(tagType.getCanonicalName(), tagType);
                    }
                    lastCacheFill.set(now());
                }
            }
        }
        return tagTypeCache.get(canonicalName);
    }

}