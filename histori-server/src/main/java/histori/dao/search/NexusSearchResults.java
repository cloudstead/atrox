package histori.dao.search;

import histori.model.Nexus;
import histori.model.support.NexusSummary;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.cobbzilla.util.collection.FieldTransfomer;
import org.cobbzilla.util.collection.mappy.MappySortedSet;
import org.cobbzilla.wizard.dao.shard.task.ShardResultCollector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class NexusSearchResults extends MappySortedSet<String, Nexus> implements ShardResultCollector<NexusSummary> {

    public NexusSearchResults(Comparator<Nexus> comparator, int maxResults) {
        super(comparator);
        this.maxResults = maxResults;
    }

    @Getter @Setter private int maxResults;

    @Override public void addResult(Object thing) {
        if (thing != null) {
            Nexus nexus = (Nexus) thing;
            put(nexus.getCanonicalName(), nexus);
        }
    }

    @Override public List<NexusSummary> getResults() {
        final List<NexusSummary> summaries = new ArrayList<>();
        for (String key : keySet()) {
            final NexusSummary summary = new NexusSummary();
            final Nexus primary = get(key);
            summary.setPrimary(primary);

            final List<String> uuids = (List<String>) CollectionUtils.collect(getAll(key), FieldTransfomer.TO_UUID);
            uuids.remove(primary.getUuid());

            summary.setOthers(uuids.toArray(new String[uuids.size()]));
            summary.setTags(null); // todo: tag summary? shows total count of all articles that have the same tag
            summaries.add(summary);
        }
        return summaries;
    }
}
