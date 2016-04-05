package histori.dao.search;

import histori.dao.NexusSummaryDAO;
import histori.model.Nexus;
import histori.model.SearchQuery;
import histori.model.support.GeoBounds;
import histori.model.support.NexusSummary;
import histori.model.support.TimeRange;
import histori.server.HistoriConfiguration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.cache.AutoRefreshingReference;
import org.cobbzilla.util.http.URIUtil;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.server.config.ElasticSearchConfig;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.IndicesAdminClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.cobbzilla.util.json.JsonUtil.*;
import static org.cobbzilla.util.string.StringUtil.trimQuotes;
import static org.elasticsearch.index.query.QueryBuilders.*;

@Repository @Slf4j
public class ElasticSearchDAO {

    public static final String ES_INDEX = "histori";
    public static final String ES_NEXUS_TYPE = "nexus";

    public static final int MAX_PUBLIC_SEARCH_RESULTS = 200;
    public static final long CLIENT_REFRESH_INTERVAL = TimeUnit.HOURS.toMillis(1);

    @Autowired private HistoriConfiguration configuration;
    @Autowired private NexusSummaryDAO summaryDAO;

    private final AutoRefreshingReference<Client> client = new AutoRefreshingReference<Client>() {
        @Override public Client refresh() { return initClient(); }
        @Override public long getTimeout() { return CLIENT_REFRESH_INTERVAL; }
    };

    private Client initClient() {

        final ElasticSearchConfig config = configuration.getElasticSearch();
        final Settings settings = Settings.settingsBuilder().put("cluster.name", config.getCluster()).build();

        TransportClient c = TransportClient.builder().settings(settings).build();
        for (String uri : config.getServers()) c = c.addTransportAddress(toTransportAddress(uri));

        try {
            final IndicesAdminClient indices = c.admin().indices();
            if (!indices.exists(new IndicesExistsRequest(ES_INDEX)).actionGet().isExists()) {
                indices.create(new CreateIndexRequest(ES_INDEX).mapping(ES_NEXUS_TYPE, getNexusMappingJson())).actionGet();
            }
        } catch (Exception e) {
            die("initClient: error setting up mappings: "+e, e);
        }

        return c;
    }

    private String getNexusMappingJson() { return loadResourceAsStringOrDie("seed/elasticsearch_nexus_mapping.json"); }

    private TransportAddress toTransportAddress(String uri) {
        try {
            return new InetSocketTransportAddress(InetAddress.getByName(URIUtil.getHost(uri)), URIUtil.getPort(uri));
        } catch (UnknownHostException e) {
            return die("toTransportAddress("+uri+"): "+e);
        }
    }

    private ExecutorService executor = Executors.newFixedThreadPool(100);

    public Future<?> index (Nexus nexus) {
        if (!nexus.isAuthoritative()) {
            log.warn("index: refusing to index non-authoritative nexus: "+nexus.getCanonicalName());
            return null;
        }
        return executor.submit(new NexusIndexJob(nexus));
    }

    public boolean delete (Nexus nexus) {
        DeleteResponse response;
        synchronized (client) {
            response = client.get().prepareDelete(ES_INDEX, ES_NEXUS_TYPE, nexus.getCanonicalName()).get();
        }
        return response.isFound();
    }

    public SearchResults<NexusSummary> search(SearchQuery searchQuery) {

        // empty query still returns nothing
        if (empty(searchQuery.getQuery())) return new SearchResults<>();

        final TimeRange range = searchQuery.getTimeRange();
        final long startInstant = range.getStartPoint().getDateInstant();
        final long endInstant = range.getEndPoint().getDateInstant();
        final GeoBounds bounds = searchQuery.getBounds();
        double north = bounds.getNorth();
        double south = bounds.getSouth();
        double east = bounds.getEast();
        double west = bounds.getWest();
        final String query = trimQuotes(searchQuery.getQuery());

        final SearchResults<NexusSummary> results = new SearchResults<>();
        final SearchRequestBuilder requestBuilder = client.get().prepareSearch(ES_INDEX).setTypes(ES_NEXUS_TYPE)
                // match query against just about anything. initial attempt at weightings. should markdown get any weight?
                // todo: allow specific SearchQuery fields to match nexusType(s), name(s), tag name(s), tag value(s), or
                // even specifically match the markdown
                .setQuery(boolQuery().must(boolQuery()
                        .should(commonTermsQuery("name", query).boost(1.5f))
                        .should(commonTermsQuery("nexusType", query).boost(2.5f))
                        .should(commonTermsQuery("canonicalName", query).boost(2.0f))
                        .should(commonTermsQuery("tags.tagType", query).boost(1.1f))
                        .should(commonTermsQuery("tags.tagName", query).boost(1.4f))
                        .should(commonTermsQuery("tags.canonicalName", query).boost(1.5f))
                        .should(commonTermsQuery("tags.values.field", query).boost(0.8f))
                        .should(commonTermsQuery("tags.values.value", query).boost(0.8f))
                        .should(commonTermsQuery("markdown", query).boost(0.3f))
                ))

                // filters are time + geo
                .setPostFilter(boolQuery()
                        // either nexus start or end must fall within query time range
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

                ).setFrom(0).setSize(MAX_PUBLIC_SEARCH_RESULTS);

        final SearchResponse response = requestBuilder.execute().actionGet();
        final SearchHits hits = response.getHits();

        for (SearchHit hit : hits) {
            final Nexus nexus = fromJsonOrDie(hit.getSourceAsString(), Nexus.class);
            results.addResult(new NexusSummary().setPrimary(nexus).initSearchUuid());
        }

        Collections.sort(results.getResults(), NexusSummary.comparator(searchQuery.getSummarySortOrder()));

        return results;
    }

    @AllArgsConstructor
    private class NexusIndexJob implements Runnable {
        private final Nexus nexus;

        @Override public void run() {
            try {
                final String json = toJson(nexus);

                final IndexRequest indexRequest = new IndexRequest(ES_INDEX, ES_NEXUS_TYPE, nexus.getCanonicalName()).source(json);
                final UpdateRequest updateRequest = new UpdateRequest(ES_INDEX, ES_NEXUS_TYPE, nexus.getCanonicalName())
                        .doc(json)
                        .upsert(indexRequest);
                final UpdateResponse response;
                synchronized (client) {
                    response = client.get().update(updateRequest).get();
                }

                if (response.getShardInfo().getSuccessful() == 0 && response.getShardInfo().getFailed() > 0) {
                    log.warn("Error indexing Nexus: "+toJsonOrErr(response));
                }

            } catch (Exception e) {
                final String msg = "index: " + e;
                log.error(msg, e);
                die(msg, e);
            }
        }
    }
}
