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
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.cobbzilla.util.string.StringUtil.trimQuotes;
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

    @Override protected SearchRequestBuilder prepareSearch(SearchQuery searchQuery, SearchRequestBuilder searchRequestBuilder) {
        final TimeRange range = searchQuery.getTimeRange();
        final long startInstant = range.getStartPoint().getDateInstant();
        final long endInstant = range.getEndPoint().getDateInstant();
        final GeoBounds bounds = searchQuery.getBounds();
        double north = bounds.getNorth();
        double south = bounds.getSouth();
        double east = bounds.getEast();
        double west = bounds.getWest();
        final String query = trimQuotes(searchQuery.getQuery());

        return searchRequestBuilder.setQuery(boolQuery().must(boolQuery()
                .should(commonTermsQuery("name", query).boost(1.5f))
                .should(commonTermsQuery("nexusType", query).boost(2.5f))
                .should(commonTermsQuery("canonicalName", query).boost(2.0f))
                .should(commonTermsQuery("tags.tagType", query).boost(1.1f))
                .should(commonTermsQuery("tags.tagName", query).boost(1.4f))
                .should(commonTermsQuery("tags.canonicalName", query).boost(1.5f))
                .should(commonTermsQuery("tags.values.field", query).boost(0.8f))
                .should(commonTermsQuery("tags.values.value", query).boost(0.8f))
                .should(commonTermsQuery("markdown", query).boost(0.1f))
        ))
                // filters are time + geo
                .setPostFilter(boolQuery()
                        // either entity start or end must fall within query time range
                        .must(boolQuery()
                                .should(rangeQuery("timeRange.startPoint.dateInstant").from(startInstant).to(endInstant))
                                .should(rangeQuery("timeRange.endPoint.dateInstant").from(startInstant).to(endInstant)))

                        // one of the corners of the bounding rectangle must be within the bounds of the query
                        // there are more complex polygon-overlap algorithms but would be much harder to implement against
                        // general-purpose tech like a search engine
                        // todo: consider: a custom geo-optimized storage engine
                        .must(boolQuery()
                                .should(geoBoundingBoxQuery("bounds.topLeft").topLeft(north, west).bottomRight(south, east))
                                .should(geoBoundingBoxQuery("bounds.topRight").topLeft(north, west).bottomRight(south, east))
                                .should(geoBoundingBoxQuery("bounds.bottomLeft").topLeft(north, west).bottomRight(south, east))
                                .should(geoBoundingBoxQuery("bounds.bottomRight").topLeft(north, west).bottomRight(south, east)))
                );
    }

}
