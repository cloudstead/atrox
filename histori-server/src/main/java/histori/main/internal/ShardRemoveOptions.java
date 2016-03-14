package histori.main.internal;

import histori.main.HistoriApiOptions;
import lombok.Getter;
import lombok.Setter;
import org.kohsuke.args4j.Option;

public class ShardRemoveOptions extends HistoriApiOptions{

    public static final String USAGE_SHARD_SET = "Name of shard set";
    public static final String OPT_SHARD_SET = "-n";
    public static final String LONGOPT_SHARD_SET= "--shard-set";
    @Option(name=OPT_SHARD_SET, aliases=LONGOPT_SHARD_SET, usage=USAGE_SHARD_SET, required=true)
    @Getter @Setter private String shardSet;

    public static final String USAGE_UUID = "UUID of the shard to remove";
    public static final String OPT_UUID = "-u";
    public static final String LONGOPT_UUID= "--uuid";
    @Option(name=OPT_UUID, aliases=LONGOPT_UUID, usage=USAGE_UUID, required=true)
    @Getter @Setter private String uuid;

}
