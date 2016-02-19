package histori.main.wiki;

import histori.wiki.WikiArchive;
import org.cobbzilla.wizard.main.MainBase;

public class ArticlePathMain extends MainBase<ArticlePathOptions> {

    public static void main (String[] args) { main(ArticlePathMain.class, args); }

    @Override protected void run() throws Exception {
        out(WikiArchive.getArticlePath(getOptions().getTitle()));
    }

}
