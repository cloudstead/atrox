package histori.wiki.finder;

import histori.model.NexusTag;
import histori.model.support.LatLon;
import histori.model.support.NexusRequest;
import histori.model.support.TimeRange;
import histori.wiki.ParsedWikiArticle;
import histori.wiki.WikiArchive;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.geojson.Point;

@Slf4j @Accessors(chain=true)
public abstract class FinderBase<T> implements WikiDataFinder<T> {

    @Getter @Setter protected ParsedWikiArticle article;
    @Getter @Setter protected WikiArchive wiki;

    public static String normalizeInfoboxName(String s) { return s.toLowerCase().replaceAll("\\s", ""); }

    public boolean addStandardTags (WikiArchive wiki, NexusRequest nexusRequest) {
        // When was it?
        TimeRange dateRange = new DateRangeFinder().setWiki(wiki).setArticle(article).find();
        if (dateRange == null) {
            String msg = "toNexusRequest: " + article.getName() + " had no date (skipping)";
            log.warn(msg);
            return false;
        }

        LatLon coordinates = getLatLon(wiki);
        if (coordinates == null) return false;

        nexusRequest.setPoint(new Point(coordinates.getLon(), coordinates.getLat()))
                .setTimeRange(dateRange)
                .setName(article.getName());
        return true;
    }

    public LatLon getLatLon(WikiArchive wiki) { return getLatLon(wiki, this.article); }

    public LatLon getLatLon(WikiArchive wiki, ParsedWikiArticle article) {
        // Where was it?
        LatLon coordinates = new LocationFinder().setWiki(wiki).setArticle(article).find();
        if (coordinates == null) {
            String msg = "toNexusRequest: " + article.getName() + " had no coordinates (skipping)";
            log.warn(msg);
            return null;
        }
        return coordinates;
    }

    public NexusTag newTag(String tagName, String tagType) {
        NexusTag tag;

        // chop anything after a trailing HTML-encoded <
        int openTagPos = tagName.indexOf("&lt;");
        if (openTagPos != -1) tagName = tagName.substring(0, openTagPos);
        tag = new NexusTag();
        tag.setTagName(tagName);
        tag.setTagType(tagType);
        return tag;
    }

    public NexusTag newTag(String tagName, String tagType, String field, String value) {
        NexusTag tag = newTag(tagName, tagType);
        tag.setValue(field, value);
        return tag;
    }

}
