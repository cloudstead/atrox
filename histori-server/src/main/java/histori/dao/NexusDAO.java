package histori.dao;

import histori.model.Account;
import histori.model.Nexus;
import histori.model.NexusTag;
import histori.model.TagType;
import histori.model.support.EntityVisibility;
import histori.model.support.GeoBounds;
import histori.model.support.TimeRange;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.cache.redis.RedisService;
import org.cobbzilla.wizard.dao.EntityFilter;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import javax.validation.Valid;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.security.ShaUtil.sha256_hex;
import static org.hibernate.criterion.Restrictions.*;

@Repository @Slf4j
public class NexusDAO extends VersionedEntityDAO<Nexus> {

    public static final int MAX_RESULTS = 200;

    @Autowired private NexusTagDAO nexusTagDAO;
    @Autowired private TagTypeDAO tagTypeDAO;
    @Autowired private RedisService redisService;

    private static final long FILTER_CACHE_TIMEOUT_SECONDS = TimeUnit.DAYS.toSeconds(1);

    @Getter(lazy=true) private final RedisService filterCache = initFilterCache();
    private RedisService initFilterCache() { return redisService.prefixNamespace(NexusEntityFilter.class.getSimpleName()); }

    @Override public Object preCreate(@Valid Nexus entity) {
        entity.prepareForSave();

        // ensure tag is present, or create it if not
        if (entity.hasNexusType()) {
            return new NexusTag().setTagName(entity.getNexusType()).setTagType(TagType.EVENT_TYPE);
        }

        return super.preCreate(entity);
    }

    @Override public Object preUpdate(@Valid Nexus entity) {
        entity.prepareForSave();

        // ensure event_type tag corresponding to nexusType is present, or create it if not
        if (entity.hasNexusType()) {
            // what tags already exist?
            final List<NexusTag> nexusTags = nexusTagDAO.findByNexusAndOwner(entity.getOwner(), entity.getUuid());
            entity.setTags(nexusTags);

            final NexusTag typeTag = (NexusTag) new NexusTag().setTagName(entity.getNexusType()).setTagType(TagType.EVENT_TYPE);
            if (!entity.hasExactTag(typeTag)) {
                entity.addTag(typeTag);
                return typeTag;
            } else {
                // nexusType already matches one of the event_type tags
            }
        } else {
            entity.setNexusType(entity.getFirstEventType());
        }

        return super.preUpdate(entity);
    }

    @Override public Nexus postCreate(Nexus entity, Object context) {
        createEventTypeTag(entity, context);
        return super.postCreate(entity, context);
    }

    @Override public Nexus postUpdate(@Valid Nexus entity, Object context) {
        createEventTypeTag(entity, context);
        return super.postUpdate(entity, context);
    }

    public void createEventTypeTag(Nexus entity, Object context) {
        if (context instanceof NexusTag) {
            final NexusTag tag = (NexusTag) context;
            // create the event_type tag
            final NexusTag typeTag = (NexusTag) tag.setNexus(entity.getUuid()).setOwner(entity.getOwner());
            if (!entity.hasExactTag(typeTag)) {
                nexusTagDAO.create(typeTag);
            }
        }
    }

    public List<Nexus> findByName(String name) { return findByField("name", name); }

    public Nexus findByOwnerAndName(Account account, String name) {
        return uniqueResult(criteria().add(and(
                eq("owner", account.getUuid()),
                eq("name", name))));
    }

    public Nexus findByOwnerAndNameOrUuid(Account account, String nameOrUuid) {
        return uniqueResult(criteria().add(and(
                eq("owner", account.getUuid()),
                or( eq("uuid", nameOrUuid),
                        eq("name", nameOrUuid) ))));
    }

    public Nexus findByOwnerAndUuid(Account account, String uuid) {
        return uniqueResult(criteria().add(and(
                eq("owner", account.getUuid()),
                eq("uuid", uuid))));
    }

    /**
     * Find all publicly-viewable nexus in the range
     * @param range the time range to search
     * @param bounds the geo bounds for the search
     * @param query a tag query
     * @return a List of Nexus objects
     */
    public List<Nexus> findByTimeRangeAndGeo(TimeRange range, GeoBounds bounds, final String query) {
        final BigInteger start = range.getStartPoint().getInstant();
        final BigInteger end = range.getEndPoint().getInstant();
        return list(criteria().add(and(
                    or(
                            and(ge("timeRange.startPoint.instant", start), le("timeRange.startPoint.instant", end)),
                            and(ge("timeRange.endPoint.instant", start), le("timeRange.endPoint.instant", end))),
                    boundsClause(bounds),
                    eq("visibility", EntityVisibility.everyone)
            )).addOrder(Order.desc("timeRange.startPoint.instant")), 0, MAX_RESULTS, new NexusEntityFilter(query));
    }

    /**
     * Find all nexus in the range that are owned by the account
     * @param account the Nexus owner
     * @param range the time range to search
     * @param visibility what kinds of nexuses to return
     * @param query a tag query
     * @return a List of Nexus objects
     */
    public List<Nexus> findByTimeRangeAndGeo(Account account, TimeRange range, GeoBounds bounds, EntityVisibility visibility, String query) {
        final BigInteger start = range.getStartPoint().getInstant();
        final BigInteger end = range.getEndPoint().getInstant();
        return list(criteria().add(and(
                    or(
                            and(ge("timeRange.startPoint.instant", start), le("timeRange.startPoint.instant", end)),
                            and(ge("timeRange.endPoint.instant", start), le("timeRange.endPoint.instant", end))),
                    boundsClause(bounds),
                    visibilityClause(account, visibility))
            ).addOrder(Order.desc("timeRange.startPoint.instant")), 0, MAX_RESULTS, new NexusEntityFilter(query));
    }

    public Criterion visibilityClause(Account account, EntityVisibility visibility) {
        if (account == null) return eq("visibility", EntityVisibility.everyone);
        switch (visibility) {
            case everyone:       return or(
                    and(eq("owner", account.getUuid()), eq("visibility", EntityVisibility.owner)),
                    eq("visibility", EntityVisibility.everyone));
            case owner: default: return and(eq("owner", account.getUuid()), eq("visibility", EntityVisibility.owner));
            case hidden:         return and(eq("owner", account.getUuid()), eq("visibility", EntityVisibility.hidden));
        }
    }

    public Criterion boundsClause(GeoBounds bounds) {
        return or(
                // check if northeast or northwest corner is within bounds
                and(le("bounds.north", bounds.getNorth()), ge("bounds.north", bounds.getSouth()),
                        or(and(le("bounds.east", bounds.getEast()), ge("bounds.east", bounds.getWest())),
                                and(le("bounds.west", bounds.getEast()), ge("bounds.west", bounds.getWest())))),

                // check if southeast or southwest corner is within bounds
                and(le("bounds.south", bounds.getNorth()), ge("bounds.south", bounds.getSouth()),
                        or(and(le("bounds.east", bounds.getEast()), ge("bounds.east", bounds.getWest())),
                                and(le("bounds.west", bounds.getEast()), ge("bounds.west", bounds.getWest()))))
        );
    }

    private class NexusEntityFilter implements EntityFilter<Nexus> {

        private final String queryHash;
        private final SortedSet<String> terms;

        public NexusEntityFilter (String query) {
            // by sorting terms alphabetically, and trimming whitespace, we prevent duplicate cache entries
            // for searches that are essentially the same
            terms = new TreeSet<>(StringUtil.splitAndTrim(query, "\n\t"));
            queryHash = sha256_hex(StringUtil.toString(terms, " "));
        }

        @Override public boolean isAcceptable(Nexus nexus) {

            if (empty(terms)) return true; // empty query matches everything

            final String cacheKey = nexus.getUuid() + ":query:" + queryHash;
            String cached = null;
            try {
                cached = getFilterCache().get(cacheKey);
            } catch (Exception e) {
                log.error("Error reading from NexusEntityFilter query cache: "+e);
            }

            if (!empty(cached)) return Boolean.valueOf(cached);

            boolean match = isMatch(nexus);

            try {
                getFilterCache().set(cacheKey, String.valueOf(match), "EX", FILTER_CACHE_TIMEOUT_SECONDS);
            } catch (Exception e) {
                log.error("Error writing to NexusEntityFilter query cache: "+e);
            }

            return match;
        }

        // todo: make this a lot better. return a score or accept some other context object so we can be smarter.
        private boolean fuzzyMatch (String s1, String s2) { return s1 != null && s1.toLowerCase().contains(s2.toLowerCase()); }
        private boolean preciseMatch (String s1, String s2) { return s1 != null && s1.equalsIgnoreCase(s2); }

        // todo: regex support/syntax for query terms?
        protected boolean isMatch(Nexus nexus) {

            // must match on all search terms
            for (String term : terms) {
                final String cacheKey = nexus.getUuid()+":term:"+sha256_hex(term);
                String cached = null;
                try {
                    cached = getFilterCache().get(cacheKey);
                } catch (Exception e) {
                    log.error("Error reading from NexusEntityFilter query-term cache: "+e);
                }

                if (!empty(cached)) return Boolean.valueOf(cached);

                boolean match = matchTerm(nexus, term);

                try {
                    getFilterCache().set(cacheKey, String.valueOf(match), "EX", FILTER_CACHE_TIMEOUT_SECONDS);
                } catch (Exception e) {
                    log.error("Error writing to NexusEntityFilter query-term cache: "+e);
                }

                if (!match) return false;
            }
            return true;
        }

        protected boolean matchTerm(Nexus nexus, String term) {
            // name match is fuzzy
            if (fuzzyMatch(nexus.getName(), term)) return true;

            // type match must be exact (ignoring case)
            if (preciseMatch(nexus.getNexusType(), term)) return true;

            // check tags
            final List<NexusTag> tags = nexusTagDAO.findByNexus(nexus.getUuid());
            for (NexusTag tag : tags) {

                // tag name match is fuzzy
                if (fuzzyMatch(tag.getTagName(), term)) return true;

                // type match must be exact (ignoring case)
                if (preciseMatch(tag.getTagType(), term)) return true;

                // all schema matches, both keys and (possibly multiple) values, are fuzzy matches
                if (tag.hasSchemaValues()) {
                    for (Map.Entry<String, Set<String>> schema : tag.getSchemaValueMap().entrySet()) {
                        if (fuzzyMatch(schema.getKey(), term)) return true;
                        for (String value : schema.getValue()) {
                            if (fuzzyMatch(value, term)) return true;
                        }
                    }
                }
            }

            // nothing matched the query term
            return false;
        }
    }
}
