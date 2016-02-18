package histori.main.wiki;

import histori.model.support.LatLon;
import histori.model.support.TimeRange;
import histori.wiki.ParsedWikiArticle;
import histori.wiki.WikiArchive;
import histori.wiki.WikiArticle;
import histori.wiki.finder.DateRangeFinder;
import histori.wiki.finder.LocationFinder;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.wizard.main.MainBase;

import java.io.File;

import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.json.JsonUtil.fromJson;

@Slf4j
public class ArticleImportMain extends MainBase<ArticleImportOptions> {

    public static void main(String[] args) { main(ArticleImportMain.class, args); }

    @Override protected void run() throws Exception {

        final ArticleImportOptions options = getOptions();
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

//            WorldEventHistory eventHistory = new WorldEventHistory();
//            eventHistory.setWorldEvent(article.getTitle());
//            eventHistory.setStartPoint(dateRange.getStart());
//            eventHistory.setEndPoint(dateRange.getEnd());

            // do we have coordinates?

            // do we have a location?
            // if so -- find location entry among other library files

            // extract combatants

            // write out a WorldEventHistory record, with dates, location, and Actors

        } catch (Exception e) {
            log.warn("Error processing article: "+parsed.getName()+": "+e, e);
        }
    }

}
