package histori.main;

import histori.main.internal.ShardBulkUpdateMain;
import histori.main.internal.ShardUpdateMain;
import histori.main.internal.ShardListMain;
import histori.main.internal.ShardRemoveMain;
import histori.main.wiki.ArticleNexusMain;
import histori.main.wiki.ArticlePathMain;
import histori.main.wiki.WikiIndexerMain;
import org.cobbzilla.util.collection.MapBuilder;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.wizard.main.MainBase;

import java.util.Map;

public class HistoriMain {

    private static Map<String, Class<? extends MainBase>> mainClasses = MapBuilder.build(new Object[][]{
            {"index",             WikiIndexerMain.class},
            {"path",              ArticlePathMain.class},
            {"nexus",             ArticleNexusMain.class},
            {"import",            NexusImportMain.class},
            {"shard-update",      ShardUpdateMain.class},
            {"shard-bulk-update", ShardBulkUpdateMain.class},
            {"shard-list",        ShardListMain.class},
            {"shard-remove",      ShardRemoveMain.class}
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
