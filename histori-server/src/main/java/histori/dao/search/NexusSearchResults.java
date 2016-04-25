package histori.dao.search;

import histori.dao.NexusDAO;
import histori.model.Account;
import histori.model.Nexus;
import histori.model.NexusView;
import histori.model.SearchQuery;
import histori.model.support.EntityVisibility;
import histori.model.support.NexusSummary;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.cobbzilla.util.collection.FieldTransfomer;
import org.cobbzilla.util.collection.mappy.MappyList;
import org.cobbzilla.util.collection.mappy.MappySortedSet;
import org.cobbzilla.wizard.dao.DAO;
import org.cobbzilla.wizard.dao.EntityFilter;
import org.cobbzilla.wizard.dao.shard.task.ShardResultCollector;
import org.cobbzilla.wizard.util.ResultCollector;

import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static org.cobbzilla.wizard.util.Await.awaitAndCollectSet;

@Slf4j
public class NexusSearchResults extends MappySortedSet<String, Nexus> implements ShardResultCollector<Nexus> {

    private Account account;
    private NexusDAO dao;
    private SearchQuery searchQuery;

    @Override public NexusDAO getDAO() { return dao; }
    @Override public void setDAO(DAO<Nexus> dao) { this.dao = (NexusDAO) dao; }

    public NexusSearchResults(Account account,
                              NexusDAO dao,
                              SearchQuery searchQuery,
                              EntityFilter<NexusView> filter,
                              int maxResults) {
        super(searchQuery.getNexusComparator());
        this.account = account;
        this.dao = dao;
        this.searchQuery = searchQuery;
        this.entityFilter = filter;
        this.maxResults = maxResults;
    }

    private int maxResults;
    @Override public int getMaxResults() { return maxResults; }
    @Override public ResultCollector setMaxResults(int maxResults) { this.maxResults = maxResults; return this; }

    @Getter private EntityFilter<NexusView> entityFilter;
    @Override public NexusSearchResults setEntityFilter(EntityFilter filter) { entityFilter = filter; return this; }

    @Override public boolean addResult(Object thing) {
        if (thing != null) {
            if (size() > getMaxResults()) {
                log.info("addResult: size exceeds "+getMaxResults()+", returning false");
                return false;
            }
            final NexusView nexus = (NexusView) thing;

            if (containsKey(nexus.getCanonicalName())) {
                log.warn("addResult: already contains SuperNexus: "+nexus.getCanonicalName());
            } else {
                // find all Nexuses with the canonical name
                final TreeSet<Nexus> sorted = new TreeSet<>(getComparator());
                sorted.addAll(getMatchingNexuses(nexus));
                if (!sorted.isEmpty()) {
                    if (entityFilter == null || entityFilter.isAcceptable(sorted.first())) {
                        putAll(nexus.getCanonicalName(), sorted);
                    }
                } else {
                    // should always have some results
                    log.warn("addResult: SuperNexus matched but no Nexuses found: " + nexus.getCanonicalName());
                }
            }
        }
        return true;
    }

    @Override public List await(List<Future<List>> futures, long timeout) throws TimeoutException {
        // collect results until we are at max, or all jobs finish. use Set to avoid re-adding duplicates
        try {
            awaitAndCollectSet(futures, getMaxResults(), timeout);
        } catch (TimeoutException e) {
            log.warn("await: timed out, returning early with "+size()+" results");
        }
        return getResults();
    }

    public List<Nexus> getMatchingNexuses(NexusView nexus) {
        final List<Nexus> found = findNexuses(nexus);
        if (searchQuery.hasBlockedOwners()) {
            for (Iterator<Nexus> iter = found.iterator(); iter.hasNext(); ) {
                final Nexus n = iter.next();
                if (searchQuery.hasBlockedOwner(n.getOwner())) {
                    iter.remove();
                }
            }
        }
        return found;
    }

    private static final MappyList<String, Nexus> nexusCache = new MappyList<>(100_000);
    public static void removeFromCache(String canonicalName) { nexusCache.remove(canonicalName); }

    protected List<Nexus> findNexuses(NexusView nexus) {
        final String prefix = "getMatchingNexuses("+nexus.getCanonicalName()+"): ";
        final EntityVisibility visibility = searchQuery.getVisibility();
        final String cacheKey = nexus.getCanonicalName()+"\t"+ visibility +"\t"+(account == null || visibility == EntityVisibility.everyone ? "null" : account.getUuid());
        if (nexusCache.containsKey(cacheKey)) {
            return nexusCache.getAll(cacheKey);

        } else {
            synchronized (nexusCache) {
                if (nexusCache.containsKey(cacheKey)) {
                    return nexusCache.getAll(cacheKey);
                } else {
                    List<Nexus> found;
                    switch (visibility) {
                        case everyone:
                            found = getDAO().findByFields("canonicalName", nexus.getCanonicalName(), "visibility", EntityVisibility.everyone);
                            break;
                        case owner:
                        case hidden:
                            if (account != null) {
                                found = getDAO().findByFields("canonicalName", nexus.getCanonicalName(), "owner", account.getUuid(), "visibility", visibility);
                            } else {
                                log.warn("getMatchingNexuses: visibility was " + visibility + " but account was null, only looking at public nexuses");
                                found = getDAO().findByFields("canonicalName", nexus.getCanonicalName(), "visibility", EntityVisibility.everyone);
                            }
                            break;
                        case deleted:
                        default:
                            log.warn(prefix + "not returning any results for visibility=" + visibility);
                            found = new ArrayList<>();
                    }
                    nexusCache.putAll(cacheKey, found);
                    return found;
                }
            }
        }
    }

    @Override public List<NexusSummary> getResults() {
        final List<NexusSummary> summaries = new ArrayList<>();
        for (String key : keySet()) {
            summaries.add(toNexusSummary(getAll(key)));
        }
        return summaries;
    }

    public static NexusSummary toNexusSummary(SortedSet<Nexus> all) {
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
