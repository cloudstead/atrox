package histori.dao.search;

import edu.emory.mathcs.backport.java.util.Collections;
import histori.dao.NexusEntityFilter;
import histori.dao.NexusSummaryDAO;
import histori.model.Nexus;
import histori.model.SearchQuery;
import histori.model.support.GeoBounds;
import histori.model.support.NexusSummary;
import histori.model.support.TimeRange;
import histori.server.HistoriConfiguration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.URIUtil;
import org.cobbzilla.util.json.JsonUtil;
import org.cobbzilla.wizard.dao.SearchResults;
import org.cobbzilla.wizard.server.config.ElasticSearchConfig;
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

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.rangeQuery;

@Repository @Slf4j
public class ElasticSearchDAO {

    public static final String ES_INDEX = "histori";
    public static final String ES_NEXUS_TYPE = "nexus";

    @Autowired private HistoriConfiguration configuration;
    @Autowired private NexusSummaryDAO summaryDAO;

    @Getter(lazy=true) private final Client client = initClient();
    private Client initClient() {

        final ElasticSearchConfig config = configuration.getElasticSearch();
        final Settings settings = Settings.settingsBuilder().put("cluster.name", config.getCluster()).build();

        TransportClient c = TransportClient.builder().settings(settings).build();
        for (String uri : config.getServers()) c = c.addTransportAddress(toTransportAddress(uri));

        return c;
    }

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
            final String json = JsonUtil.toJsonOrDie(nexus);

            final IndexRequest indexRequest = new IndexRequest(ES_INDEX, ES_NEXUS_TYPE, nexus.getCanonicalName()).source(json);
            final UpdateRequest updateRequest = new UpdateRequest(ES_INDEX, ES_NEXUS_TYPE, nexus.getCanonicalName())
                    .doc(json)
                    .upsert(indexRequest);
            final UpdateResponse response = getClient().update(updateRequest).get();

            if (!response.isCreated()) {
                log.warn("Error indexing Nexus: "+response);
            }

        } catch (Exception e) {
            die("index: "+e, e);
        }
    }

    public boolean delete (Nexus nexus) {
        DeleteResponse response = getClient().prepareDelete(ES_INDEX, ES_NEXUS_TYPE, nexus.getCanonicalName()).get();
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
        final SearchResponse response = getClient().prepareSearch(ES_INDEX).setTypes(ES_NEXUS_TYPE)
                .setPostFilter(boolQuery()
                        // either nexus start or end must fall within query time range
                        .must(boolQuery()
                                .should(rangeQuery("range.startPoint.dateInstant").from(startInstant).to(endInstant))
                                .should(rangeQuery("range.endPoint.dateInstant")  .from(startInstant).to(endInstant)))
                        // one of the corners of the bounding rectangle must be within the bounds of the query
                        // there are more complex polygon-overlap algorithms but would be much harder to implement against
                        // general-purpose tech like a search engine
                        // todo: consider: a custom geo-optimized storage engine
                        .must(boolQuery()
                                .should(boolQuery()
                                        .must(rangeQuery("bounds.north").from(south).to(north))
                                        .should(boolQuery()
                                                .must(rangeQuery("bounds.east").from(east).to(west))
                                                .must(rangeQuery("bounds.west").from(east).to(west))))
                                .should(boolQuery()
                                        .must(rangeQuery("bounds.south").from(south).to(north))
                                        .should(boolQuery()
                                                .must(rangeQuery("bounds.east").from(east).to(west))
                                                .must(rangeQuery("bounds.west").from(east).to(west)))))

                ).setFrom(0).setSize(NexusSummaryDAO.MAX_SEARCH_RESULTS)
                .execute().actionGet();

        final NexusEntityFilter entityFilter = new NexusEntityFilter(searchQuery.getQuery(),
                                                                     summaryDAO.getFilterCache(),
                                                                     summaryDAO.getTagDAO(),
                                                                     summaryDAO.getTagTypeDAO());

        final SearchResults<NexusSummary> results = new SearchResults<>();
        for (SearchHit hit : response.getHits()) {
            final Nexus nexus = JsonUtil.fromJsonOrDie(hit.getSourceAsString(), Nexus.class);
            if (entityFilter.isAcceptable(nexus)) {
                results.addResult(new NexusSummary().setPrimary(nexus));
            }
        }
        Collections.sort(results.getResults(), NexusSummary.comparator(searchQuery.getSummarySortOrder()));

        return results;
    }

}
