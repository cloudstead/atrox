package atrox.main.wiki;

import atrox.model.history.WorldEventHistory;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.wizard.main.MainBase;

import java.io.File;

import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.json.JsonUtil.fromJson;

public class BattleImportMain extends MainBase<BattleImportOptions> {

    public static final String INFOBOX_MILITARY_CONFLICT = "{{Infobox military conflict";
    public static final String DATE_PREFIX = "|date=";

    public static void main(String[] args) { main(BattleImportMain.class, args); }

    @Override protected void run() throws Exception {

        final BattleImportOptions options = getOptions();
        final File file = options.getFile();

        final WikiArticle article = fromJson(FileUtil.toString(file), WikiArticle.class);
        if (article.getText().toLowerCase().startsWith("#redirect")) {
            out(abs(file)+ " is a redirect. exiting");
            return;
        }

        final String text = article.getText();
//        WikiBlock infoBlock = article.getBlock("Infobox military conflict");
//        WikiMilitaryConflict conflict = new WikiMilitaryConflict(infoBlock);

        WorldEventHistory eventHistory = new WorldEventHistory();
        eventHistory.setWorldEvent(article.getTitle());
//        eventHistory.setStartPoint(conflict.getDateStart());
//        eventHistory.setEndPoint(conflict.getDateEnd());

        // do we have coordinates?

        // do we have a location?
        // if so -- find location entry among other library files

        // extract combatants

        // write out a WorldEventHistory record, with dates, location, and Actors
    }

}
