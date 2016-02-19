package histori.main;

import histori.main.wiki.ArticleFilterMain;
import histori.main.wiki.ArticleNexusMain;
import histori.main.wiki.MasterDbSplitterMain;
import org.cobbzilla.util.collection.MapBuilder;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.main.MainBase;

import java.util.Map;

public class HistoriMain {

    private static Map<String, Class<? extends MainBase>> mainClasses = MapBuilder.build(new Object[][]{
            {"split",  MasterDbSplitterMain.class},
            {"filter", ArticleFilterMain.class},
            {"nexus",  ArticleNexusMain.class}
    });

    public static void main(String[] args) throws Exception {

        if (args.length == 0) die("No command provided, use one of: "+ StringUtil.toString(mainClasses.keySet(), " "));

        // extract command
        final String command = args[0];
        final Class mainClass = mainClasses.get(command.toLowerCase());
        if (mainClass == null) die("Command not found: "+command+", use one of: "+StringUtil.toString(mainClasses.keySet(), " "));

        // shift
        final String[] newArgs = new String[args.length-1];
        System.arraycopy(args, 1, newArgs, 0, args.length-1);

        // invoke "real" main
        mainClass.getMethod("main", String[].class).invoke(null, (Object) newArgs);
    }

    private static void die(String s) {
        System.out.println(s);
        System.exit(1);
    }
}
