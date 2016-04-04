package histori.dao.search;

import histori.model.support.NexusSummary;
import lombok.Getter;
import org.cobbzilla.wizard.dao.SearchResults;

import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.daemon.ZillaRuntime.now;

public class CachedSearchResults {

    @Getter private final AtomicReference<SearchResults<NexusSummary>> results = new AtomicReference<>();
    public boolean hasResults() { return results.get() != null; }

    private long ctime = now();
    public long getAge () { return now() - ctime; }

}
