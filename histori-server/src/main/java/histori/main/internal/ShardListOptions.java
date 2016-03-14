package histori.main.internal;

import histori.main.HistoriApiOptions;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

public class ShardListOptions extends HistoriApiOptions {

    public static final String USAGE_SHARD_SET = "Name of the shard set";
    public static final String OPT_SHARD_SET = "-n";
    public static final String LONGOPT_SHARD_SET= "--shard-set";
    @Option(name=OPT_SHARD_SET, aliases=LONGOPT_SHARD_SET, usage=USAGE_SHARD_SET)
    @Getter @Setter private String shardSet;

    public boolean hasShardSet() { return shardSet != null; }
}
