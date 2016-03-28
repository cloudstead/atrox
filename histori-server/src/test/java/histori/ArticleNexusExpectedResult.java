package histori;

import histori.model.NexusTag;
import histori.model.support.LatLon;
import histori.model.support.NexusRequest;
import histori.model.support.TimeRange;
import org.cobbzilla.util.math.Cardinal;
import org.geojson.Point;

import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;
import static org.cobbzilla.util.json.JsonUtil.toJsonOrDie;
import static org.junit.Assert.*;

public class ArticleNexusExpectedResult {

    public String title;
    public boolean fullCheck;

    public boolean unparseable = false;

    public ArticleNexusExpectedResult unparseable(boolean b) {
        this.unparseable = b;
        return this;
    }

    public ArticleNexusExpectedResult(String title) {
        this(title, true);
    }

    public ArticleNexusExpectedResult(String title, boolean fullCheck) {
        this.title = title;
        this.fullCheck = fullCheck;
    }

    public LatLon location;

    public List<ArticleNexusExpectedResult> multi;

    public ArticleNexusExpectedResult getExpectedRequest(String name) {
        for (ArticleNexusExpectedResult r : multi) {
            if (r.title.equals(name)) return r;
        }
        return null;
    }

    public ArticleNexusExpectedResult nexus(ArticleNexusExpectedResult result) {
        if (multi == null) multi = new ArrayList<>();
        multi.add(result);
        return this;
    }

    public ArticleNexusExpectedResult location(LatLon location) {
        this.location = location;
        return this;
    }

    public ArticleNexusExpectedResult location(double lat, double lon) {
        return location(new LatLon(lat, lon));
    }

    public ArticleNexusExpectedResult location(double lat, Cardinal latDir, double lon, Cardinal lonDir) {
        return location(new LatLon(lat * (double) latDir.getDirection(), lon * (double) lonDir.getDirection()));
    }

    public ArticleNexusExpectedResult location(int latDeg, Integer latMin, Integer latSec, Cardinal latDir, int lonDeg, Integer lonMin, Integer lonSec, Cardinal lonDir) {
        this.location = new LatLon(latDeg, latMin, latSec, latDir, lonDeg, lonMin, lonSec, lonDir);
        return this;
    }

    public ArticleNexusExpectedResult location(double latDeg, Double latMin, Double latSec, Cardinal latDir, double lonDeg, Double lonMin, Double lonSec, Cardinal lonDir) {
        this.location = new LatLon(latDeg, latMin, latSec, latDir, lonDeg, lonMin, lonSec, lonDir);
        return this;
    }

    public ArticleNexusExpectedResult location(int latDeg, Integer latMin, Cardinal latDir, int lonDeg, Integer lonMin, Cardinal lonDir) {
        return location(latDeg, latMin, null, latDir, lonDeg, lonMin, null, lonDir);
    }

    public String getGeoJson() {
        return toJsonOrDie(new Point(location.getLon(), location.getLat()));
    }

    public TimeRange range;

    public ArticleNexusExpectedResult range(String date) {
        this.range = new TimeRange(date);
        return this;
    }

    public ArticleNexusExpectedResult range(String start, String end) {
        this.range = new TimeRange(start, end);
        return this;
    }

    public List<NexusTag> tags = new ArrayList<>();

    public ArticleNexusExpectedResult tag(String tagType, String tagName) {
        tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName));
        return this;
    }

    public ArticleNexusExpectedResult tag(String tagType, String tagName, String field1, String value1) {
        tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName).setValue(field1, value1));
        return this;
    }

    public ArticleNexusExpectedResult tag(String tagType, String tagName, String field1, String value1, String f2, String v2) {
        tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName).setValue(field1, value1).setValue(f2, v2));
        return this;
    }

    public ArticleNexusExpectedResult tag(String tagType, String tagName, String field1, String value1, String f2, String v2, String f3, String v3) {
        tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName).setValue(field1, value1).setValue(f2, v2).setValue(f3, v3));
        return this;
    }

    public ArticleNexusExpectedResult tag(String tagType, String tagName, String field1, String value1, String f2, String v2, String f3, String v3, String f4, String v4) {
        tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName).setValue(field1, value1).setValue(f2, v2).setValue(f3, v3).setValue(f4, v4));
        return this;
    }

    public ArticleNexusExpectedResult tag(String tagType, String tagName, String field1, String value1, String f2, String v2, String f3, String v3, String f4, String v4, String f5, String v5) {
        tags.add((NexusTag) new NexusTag().setTagType(tagType).setTagName(tagName).setValue(field1, value1).setValue(f2, v2).setValue(f3, v3).setValue(f4, v4).setValue(f5, v5));
        return this;
    }

    public String markdown;
    public ArticleNexusExpectedResult markdown(String md) { this.markdown = md; return this; }

    public boolean assertSameLocation(String geoJson) {
        // Always assume a point for now
        final Point p = fromJsonOrDie(geoJson, Point.class);
        assertNotNull(p);
        assertNotNull(p.getCoordinates());
        assertEquals("Latitude was off by too much", p.getCoordinates().getLatitude(), location.getLat(), 0.0001);
        assertEquals("Longitude was off by too much", p.getCoordinates().getLongitude(), location.getLon(), 0.0001);
        return true;
    }

    public void verify(NexusRequest nexusRequest) {
        if (unparseable) {
            assertNull("parsed article that should have been unparseable: " + title, nexusRequest);
        } else {
            assertNotNull("error parsing article: " + title, nexusRequest);
        }
        if (location != null) assertSameLocation(nexusRequest.getGeoJson());
        if (range != null) assertEquals(range, nexusRequest.getTimeRange());
        if (fullCheck) assertEquals("wrong # of tags for "+ title, tags.size(), nexusRequest.getTagCount());
        for (NexusTag tag : tags) {
            assertTrue("missing tag ("+ title+"): "+tag.getTagType()+"/"+tag.getTagName(), nexusRequest.hasTag(tag.getTagName()));
            assertTrue("tag doesn't match ("+ title+"): "+tag.getTagName(), nexusRequest.hasExactTag(tag));
        }
        if (markdown != null) {
            assertNotNull("markdown was null", nexusRequest.getMarkdown());
            assertTrue(nexusRequest.getMarkdown().equals(markdown));
        }
    }
}
