package histori.dao.search;

import histori.dao.NexusEntityFilter;
import histori.dao.NexusSummaryDAO;
import histori.model.Nexus;
import histori.model.SearchQuery;
import histori.model.support.GeoBounds;
import histori.model.support.NexusSummary;
import histori.model.support.TimeRange;
import histori.server.HistoriConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.cache.AutoRefreshingReference;
import org.cobbzilla.util.http.URIUtil;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.server.config.ElasticSearchConfig;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.StreamUtil.loadResourceAsStringOrDie;
import static org.cobbzilla.util.json.JsonUtil.*;
import static org.elasticsearch.index.query.QueryBuilders.*;

@Repository @Slf4j
public class ElasticSearchDAO {

    public static final String ES_INDEX = "histori";
    public static final String ES_NEXUS_TYPE = "es-nexus";
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
            c.admin().indices().putMapping(new PutMappingRequest(ES_INDEX).source(getMappingJson()));
        } catch (Exception e) {
            die("initClient: error setting up mappings: "+e, e);
        }

        return c;
    }

    private String getMappingJson() { return loadResourceAsStringOrDie("seed/elasticsearch_mapping.json"); }

    private TransportAddress toTransportAddress(String uri) {
        try {
            return new InetSocketTransportAddress(InetAddress.getByName(URIUtil.getHost(uri)), URIUtil.getPort(uri));
        } catch (UnknownHostException e) {
            return die("toTransportAddress("+uri+"): "+e);
        }
    }

    public void index (Nexus nexus) {
        if (!nexus.isAuthoritative()) {
            log.warn("index: refusing to index non-authoritative nexus: "+nexus.getCanonicalName());
            return;
        }
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

            if (response.getShardInfo().getSuccessful() == 0) {
                log.warn("Error indexing Nexus: "+toJsonOrErr(response));
            }

        } catch (Exception e) {
            die("index: "+e, e);
        }
    }

    public boolean delete (Nexus nexus) {
        DeleteResponse response;
        synchronized (client) {
            response = client.get().prepareDelete(ES_INDEX, ES_NEXUS_TYPE, nexus.getCanonicalName()).get();
        }
        return response.isFound();
    }

    public SearchResults<NexusSummary> search(SearchQuery searchQuery) {
        final TimeRange range = searchQuery.getTimeRange();
        final long startInstant = range.getStartPoint().getDateInstant();
        final long endInstant = range.getEndPoint().getDateInstant();
        final GeoBounds bounds = searchQuery.getBounds();
        double north = bounds.getNorth();
        double south = bounds.getSouth();
        double east = bounds.getEast();
        double west = bounds.getWest();
        if (bounds.getSouth() < bounds.getNorth()) {
            south = bounds.getNorth();
            north = bounds.getSouth();
        }
        if (bounds.getWest() < bounds.getEast()) {
            east = bounds.getWest();
            west = bounds.getEast();
        }
        final SearchResponse response = client.get().prepareSearch(ES_INDEX).setTypes(ES_NEXUS_TYPE)
                .setPostFilter(boolQuery()
                        // either nexus start or end must fall within query time range
                        .must(boolQuery()
                                .should(rangeQuery("range.startPoint.dateInstant").from(startInstant).to(endInstant))
                                .should(rangeQuery("range.endPoint.dateInstant").from(startInstant).to(endInstant)))

                        // one of the corners of the bounding rectangle must be within the bounds of the query
                        // there are more complex polygon-overlap algorithms but would be much harder to implement against
                        // general-purpose tech like a search engine
                        // todo: consider: a custom geo-optimized storage engine
                        .must(boolQuery()
                                .should(geoBoundingBoxQuery("bounds.topLeft").topLeft(north, west).bottomRight(south, east))
                                .should(geoBoundingBoxQuery("bounds.topRight").topLeft(north, west).bottomRight(south, east))
                                .should(geoBoundingBoxQuery("bounds.bottomLeft").topLeft(north, west).bottomRight(south, east))
                                .should(geoBoundingBoxQuery("bounds.bottomRight").topLeft(north, west).bottomRight(south, east)))

                ).setFrom(0).setSize(10 * NexusSummaryDAO.MAX_SEARCH_RESULTS)
                .execute().actionGet();

        final NexusEntityFilter entityFilter = new NexusEntityFilter(searchQuery.getQuery(),
                                                                     summaryDAO.getFilterCache(),
                                                                     summaryDAO.getTagDAO(),
                                                                     summaryDAO.getTagTypeDAO());

        final SearchResults<NexusSummary> results = new SearchResults<>();
        for (SearchHit hit : response.getHits()) {
            final Nexus nexus = fromJsonOrDie(hit.getSourceAsString(), Nexus.class);
            if (entityFilter.isAcceptable(nexus)) {
                results.addResult(new NexusSummary().setPrimary(nexus));
                if (results.getResults().size() > NexusSummaryDAO.MAX_SEARCH_RESULTS) break;
            }
        }
        Collections.sort(results.getResults(), NexusSummary.comparator(searchQuery.getSummarySortOrder()));

        return results;
    }

}
