package histori.main.internal;

import histori.main.HistoriApiOptions;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.io.StreamUtil;
import org.cobbzilla.wizard.model.shard.ShardMap;
import org.kohsuke.args4j.Option;

import java.io.File;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.json.JsonUtil.fromJsonOrDie;

public class ShardBulkUpdateOptions extends HistoriApiOptions {

    public static final String USAGE_SHARD_SET = "Shard set name. Default is to use the name of the first shard from input";
    public static final String OPT_SHARD_SET = "-n";
    public static final String LONGOPT_SHARD_SET= "--shard-set";
    @Option(name=OPT_SHARD_SET, aliases=LONGOPT_SHARD_SET, usage=USAGE_SHARD_SET)
    @Setter private String shardSetName = null;

    public String getShardSetName() { return shardSetName == null ? getImpliedShardSetName() : shardSetName; }

    public static final String USAGE_SHARD_SET_FILE = "JSON file containing shard set definition. If not present, reads from stdin.";
    public static final String OPT_SHARD_SET_FILE = "-f";
    public static final String LONGOPT_SHARD_SET_FILE= "--file";
    @Option(name=OPT_SHARD_SET_FILE, aliases=LONGOPT_SHARD_SET_FILE, usage=USAGE_SHARD_SET_FILE)
    @Getter @Setter private File file = null;

    public String getImpliedShardSetName () { return getShardSet()[0].getShardSet(); }

    @Getter(lazy=true) private final ShardMap[] shardSet = initShardSet();
    public ShardMap[] initShardSet() {
        final ShardMap[] shards;
        if (file != null) {
            shards = fromJsonOrDie(file, ShardMap[].class);
        } else {
            shards = fromJsonOrDie(StreamUtil.toStringOrDie(System.in), ShardMap[].class);
        }
        if (empty(shards)) die("no shards defined in input");

        if (shardSetName == null) {
            final String setName = shards[0].getShardSet();
            if (empty(setName)) die("no shard set name specified (via "+OPT_SHARD_SET+"/"+LONGOPT_SHARD_SET+") and input shards had no name");
            for (int i = 1; i < shards.length; i++) {
                if (!shards[i].getShardSet().equals(setName)) die("shards in set must all have the same shardSet name");
            }
        } else {
            for (ShardMap shard : shards) shard.setShardSet(shardSetName);
        }
        return shards;
    }
}
