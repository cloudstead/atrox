package histori.dao.search;

import histori.dao.NexusDAO;
import histori.model.Nexus;
import histori.model.NexusView;
import histori.model.support.NexusSummary;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.cobbzilla.util.collection.FieldTransfomer;
import org.cobbzilla.util.collection.mappy.MappySortedSet;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.dao.EntityFilter;
import org.cobbzilla.wizard.dao.shard.task.ShardResultCollector;
import org.cobbzilla.wizard.util.ResultCollector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

public class NexusSearchResults extends MappySortedSet<String, Nexus> implements ShardResultCollector<Nexus> {

    private NexusDAO dao;
    @Override public NexusDAO getDAO() { return dao; }
    @Override public void setDAO(DAO<Nexus> dao) { this.dao = (NexusDAO) dao; }

    public NexusSearchResults(NexusDAO dao, EntityFilter<NexusView> filter, Comparator<NexusView> comparator, int maxResults, List<String> blockedOwnersList) {
        super(comparator);
        this.dao = dao;
        this.entityFilter = filter;
        this.maxResults = maxResults;
        this.blockedOwnersList = blockedOwnersList;
    }

    private int maxResults;
    @Override public int getMaxResults() { return maxResults; }
    @Override public ResultCollector setMaxResults(int maxResults) { this.maxResults = maxResults; return this; }

    @Getter private EntityFilter<NexusView> entityFilter;
    @Override public NexusSearchResults setEntityFilter(EntityFilter filter) { entityFilter = filter; return this; }

    private List<String> blockedOwnersList;

    @Override public boolean addResult(Object thing) {
        if (thing != null) {
            if (size() > getMaxResults()) return false;
            final NexusView nexus = (NexusView) thing;
            if (blockedOwnersList == null || nexus.getOwner() == null || !blockedOwnersList.contains(nexus.getOwner())) {
                if (entityFilter != null && entityFilter.isAcceptable(nexus)) {
                    if (size() > getMaxResults()) {
                        return false;
                    }
                    boolean doit = false;
                    synchronized (this) {
                        if (!containsKey(nexus.getCanonicalName())) {
                            // other threads can stop bothering with this name
                            final Nexus placeholderNexus = (Nexus) new Nexus().setName("-in-process-");
                            placeholderNexus.setUuid("-in-process-");
                            put(nexus.getCanonicalName(), placeholderNexus);
                            doit = true;
                        }
                    }
                    if (doit) {
                        // search for this name
                        final List<Nexus> withName = getDAO().findByField("canonicalName", nexus.getCanonicalName());
                        remove(nexus.getCanonicalName());
                        putAll(nexus.getCanonicalName(), withName);
                    }
                }
            }
        }
        return true;
    }

    @Override public List<NexusSummary> getResults() {
        final List<NexusSummary> summaries = new ArrayList<>();
        for (String key : keySet()) {
            summaries.add(toNexusSummary(getAll(key)));
        }
        return summaries;
    }

    public static NexusSummary toNexusSummary(TreeSet<Nexus> all) {
        final Nexus primary = all.first();
        final NexusSummary summary = new NexusSummary();
        summary.setPrimary(primary);
        primary.getTags(); // force tag initialization from tagsJson

        final List<String> uuids = (List<String>) CollectionUtils.collect(all, FieldTransfomer.TO_UUID);
        uuids.remove(primary.getUuid());

        summary.setOthers(uuids.toArray(new String[uuids.size()]));
        summary.setTags(null); // todo: tag summary? shows total count of all articles that have the same tag
        return summary;
    }

}
