package histori.main.wiki;

import histori.main.HistoriApiMain;
import histori.model.support.LatLon;
import histori.model.support.NexusRequest;
import histori.model.support.TimeRange;
import histori.wiki.ParsedWikiArticle;
import histori.wiki.WikiArchive;
import histori.wiki.WikiArticle;
import histori.wiki.finder.DateRangeFinder;
import histori.wiki.finder.LocationFinder;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.http.HttpStatusCodes;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.wizard.client.ApiClientBase;
import org.cobbzilla.wizard.util.RestResponse;
import org.geojson.Point;

import java.io.File;

import static histori.ApiConstants.NEXUS_ENDPOINT;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.json.JsonUtil.fromJson;
import static org.cobbzilla.util.json.JsonUtil.toJson;
import static org.cobbzilla.util.string.StringUtil.urlEncode;

@Slf4j
public class ArticleImportMain extends HistoriApiMain<ArticleImportOptions> {

    public static void main(String[] args) { main(ArticleImportMain.class, args); }

    @Override protected void run() throws Exception {

        final ArticleImportOptions options = getOptions();
        final ApiClientBase api = getApiClient();
        final File file = options.getFile();

        final WikiArticle article = fromJson(FileUtil.toString(file), WikiArticle.class);
        if (article.getText().toLowerCase().startsWith("#redirect")) {
            out(abs(file)+ " is a redirect (exiting)");
            return;
        }

        final ParsedWikiArticle parsed = article.parse();
        final WikiArchive wiki = options.getWiki();
        final TimeRange dateRange;
        final LatLon coordinates;

        // When was it?
        try {
            dateRange = new DateRangeFinder().setWiki(wiki).setArticle(parsed).find();

            // Where was it?
            coordinates = new LocationFinder().setWiki(wiki).setArticle(parsed).find();

            NexusRequest nexusRequest = (NexusRequest) new NexusRequest()
                    .setPoint(new Point(coordinates.getLon(), coordinates.getLat()))
                    .setTimeRange(dateRange)
                    .setName(parsed.getName());

            // define nexus
            RestResponse response = api.doPut(NEXUS_ENDPOINT + "/" + urlEncode(nexusRequest.getName()), toJson(nexusRequest));
            if (response.status != HttpStatusCodes.OK) {
                die("Error creating nexus: "+response);
            }

            // extract combatants

            // write out a WorldEventHistory record, with dates, location, and Actors

        } catch (Exception e) {
            log.warn("Error processing article: "+parsed.getName()+": "+e, e);
        }
    }

}
