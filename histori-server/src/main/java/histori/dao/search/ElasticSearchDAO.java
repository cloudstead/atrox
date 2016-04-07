package histori.dao.search;

import histori.model.Nexus;
import histori.model.SearchQuery;
import histori.model.support.GeoBounds;
import histori.model.support.NexusSummary;
import histori.model.support.TimeRange;
import histori.server.HistoriConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.wizard.dao.AbstractElasticSearchDAO;
import org.cobbzilla.wizard.server.config.ElasticSearchConfig;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.elasticsearch.index.query.QueryBuilders.*;

@Repository @Slf4j
public class ElasticSearchDAO extends AbstractElasticSearchDAO<Nexus, SearchQuery, NexusSummary> {

    public static final String ES_INDEX = "histori";
    public static final String ES_NEXUS_TYPE = "nexus";
    public static final int MAX_PUBLIC_SEARCH_RESULTS = 200;
    public static final long CLIENT_REFRESH_INTERVAL = TimeUnit.HOURS.toMillis(1);

    @Override public String getIndexName() { return ES_INDEX; }
    @Override public String getTypeName() { return ES_NEXUS_TYPE; }
    @Override public int getMaxResults() { return MAX_PUBLIC_SEARCH_RESULTS; }
    @Override public long getClientRefreshInterval() { return CLIENT_REFRESH_INTERVAL; }

    @Override protected ElasticSearchConfig getConfiguration() { return configuration.getElasticSearch(); }
    @Override protected String getTypeMappingJson() { return loadResourceAsStringOrDie("seed/elasticsearch_nexus_mapping.json"); }
    @Override protected String getSearchId(Nexus nexus) { return nexus == null ? null : nexus.getCanonicalName(); }

    @Autowired private HistoriConfiguration configuration;

    @Override protected boolean shouldIndex(Nexus nexus) { return nexus.isAuthoritative(); }

    @Override protected boolean isEmptyQuery(SearchQuery searchQuery) { return empty(searchQuery.getQuery()); }

    @Override protected NexusSummary toSearchResult(Nexus entity) {
        return new NexusSummary().setPrimary(entity).initSearchUuid();
    }

    @Override protected Comparator<? super NexusSummary> getComparator(SearchQuery searchQuery) {
        return NexusSummary.comparator(searchQuery.getSummarySortOrder());
    }

    @Override protected QueryBuilder getQuery(SearchQuery searchQuery) {
        // add query terms
        BoolQueryBuilder query = boolQuery();
        final NexusQueryTerms terms = new NexusQueryTerms(searchQuery.getQuery());
        for (NexusQueryTerm term : terms) {
            query = query.should(nexusTermQuery(term));
        }
        return query;
    }

    @Override protected QueryBuilder getPostFilter(SearchQuery searchQuery) {
        // basic query is geo + time
        BoolQueryBuilder query = basicBoundsQuery(searchQuery);

        // add query terms
        final NexusQueryTerms terms = new NexusQueryTerms(searchQuery.getQuery());
        for (NexusQueryTerm term : terms) {
            if (term.isExact()) query = query.must(nexusTermQuery(term));
        }

        return query;
    }

    private QueryBuilder nexusTermQuery(NexusQueryTerm term) {
        BoolQueryBuilder b = boolQuery();
        if (term.isName()) b = b.should(singleTerm("name", term)).should(singleTerm("canonicalName", term));
        if (term.isNexusType()) b = b.should(singleTerm("nexusType", term));
        if (term.isMarkdown()) b = b.should(singleTerm("markdown", term));
        if (term.isTagType()) b = b.should(singleTerm("tags.tagType", term));
        if (term.isTagName()) b = b.should(singleTerm("tags.tagName", term));
        if (term.isDecoratorName()) b = b.should(singleTerm("tags.values.field", term));
        if (term.isDecoratorValue()) b = b.should(singleTerm("tags.values.value", term));
        return b;
    }

    private QueryBuilder singleTerm(String field, NexusQueryTerm term) {
        if (term.isExact()) return termQuery(field, term.getTerm());
        if (term.isFuzzy()) return commonTermsQuery(field, term.getTerm());
        if (term.isRegex()) return regexpQuery(field, term.getTerm());
        log.warn("singleTerm("+field+", "+term+"): unrecognized match type");
        return commonTermsQuery(field, term.getTerm());
    }

    private BoolQueryBuilder basicBoundsQuery(SearchQuery searchQuery) {
        final TimeRange range = searchQuery.getTimeRange();
        final long startInstant = range.getStartPoint().getDateInstant();
        final long endInstant = range.getEndPoint().getDateInstant();
        final GeoBounds bounds = searchQuery.getBounds();
        double north = bounds.getNorth();
        double south = bounds.getSouth();
        double east = bounds.getEast();
        double west = bounds.getWest();
        return boolQuery()
                // either entity start or end must fall within query time range
                .must(boolQuery()
                        .should(rangeQuery("timeRange.startPoint.dateInstant").from(startInstant).to(endInstant))
                        .should(rangeQuery("timeRange.endPoint.dateInstant").from(startInstant).to(endInstant))
                )

                // one of the corners of the bounding rectangle must be within the bounds of the query
                // there are more complex polygon-overlap algorithms but would be much harder to implement against
                // general-purpose tech like a search engine
                // todo: consider: a custom geo-optimized storage engine
                .must(boolQuery()
                        .should(geoBoundingBoxQuery("bounds.topLeft").topLeft(north, west).bottomRight(south, east))
                        .should(geoBoundingBoxQuery("bounds.topRight").topLeft(north, west).bottomRight(south, east))
                        .should(geoBoundingBoxQuery("bounds.bottomLeft").topLeft(north, west).bottomRight(south, east))
                        .should(geoBoundingBoxQuery("bounds.bottomRight").topLeft(north, west).bottomRight(south, east))
                );
    }
}
