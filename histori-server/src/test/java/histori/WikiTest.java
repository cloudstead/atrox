package histori;

import cloudos.service.asset.ResourceStorageService;
import histori.wiki.WikiArchive;
import org.cobbzilla.util.system.CommandShell;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class WikiTest {

    protected WikiArchive wiki = new WikiArchive(new ResourceStorageService("wiki/index"),
            CommandShell.loadShellExportsOrDie(ApiClientTestBase.ENV_EXPORT_FILE).get(ApiClientTestBase.ENV_PLACES_API_KEY));

    public ArticleNexusExpectedResult findTest(String title, ArticleNexusExpectedResult[] tests) {
        for (ArticleNexusExpectedResult p : tests) if (p.title.equals(title)) return p;
        return die("findTest: not found: "+title);
    }

}
