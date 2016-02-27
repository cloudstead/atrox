package histori.wiki.linematcher;

import histori.wiki.finder.InfoboxNames;

public class MilitaryConflictInfoboxLineMatcher implements LineMatcher {

    @Override public boolean matches(String line) { return line.toLowerCase().contains(InfoboxNames.INFOBOX_MILITARY_CONFLICT.toLowerCase()); }

}
