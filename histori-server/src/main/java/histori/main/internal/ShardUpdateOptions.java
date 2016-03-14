package histori.main.internal;

import histori.main.HistoriApiOptions;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.wizard.model.shard.ShardMap;
import org.cobbzilla.wizard.model.shard.ShardRange;
import org.kohsuke.args4j.Option;

public class ShardUpdateOptions extends HistoriApiOptions {

    public static final String USAGE_UUID = "UUID of the shard to update";
    public static final String OPT_UUID = "-u";
    public static final String LONGOPT_UUID= "--uuid";
    @Option(name=OPT_UUID, aliases=LONGOPT_UUID, usage=USAGE_UUID)
    @Getter @Setter private String uuid;
    public boolean hasUuid () { return uuid != null; }

    public static final String USAGE_SHARD_SET = "Name of shard set";
    public static final String OPT_SHARD_SET = "-n";
    public static final String LONGOPT_SHARD_SET= "--shard-set";
    @Option(name=OPT_SHARD_SET, aliases=LONGOPT_SHARD_SET, usage=USAGE_SHARD_SET, required=true)
    @Getter @Setter private String shardSet;

    public static final String USAGE_START = "Starting logical shard number, inclusive";
    public static final String OPT_START = "-S";
    public static final String LONGOPT_START= "--start";
    @Option(name=OPT_START, aliases=LONGOPT_START, usage=USAGE_START, required=true)
    @Getter @Setter private int logicalStart;

    public static final String USAGE_END = "Ending logical shard number, exclusive";
    public static final String OPT_END = "-E";
    public static final String LONGOPT_END= "--end";
    @Option(name=OPT_END, aliases=LONGOPT_END, usage=USAGE_END, required=true)
    @Getter @Setter private int logicalEnd;

    public static final String USAGE_URL = "JDBC URL to use for the shard";
    public static final String OPT_URL = "-U";
    public static final String LONGOPT_URL= "--url";
    @Option(name=OPT_URL, aliases=LONGOPT_URL, usage=USAGE_URL, required=true)
    @Getter @Setter private String url;

    public static final String USAGE_READ = "Enable reads";
    public static final String OPT_READ = "-R";
    public static final String LONGOPT_READ= "--read";
    @Option(name=OPT_READ, aliases=LONGOPT_READ, usage=USAGE_READ, required=true)
    @Getter @Setter private boolean allowRead;

    public static final String USAGE_WRITE = "Enable writes";
    public static final String OPT_WRITE = "-W";
    public static final String LONGOPT_WRITE= "--write";
    @Option(name=OPT_WRITE, aliases=LONGOPT_WRITE, usage=USAGE_WRITE, required=true)
    @Getter @Setter private boolean allowWrite;

    public ShardMap getShardMap() {
        return new ShardMap()
                .setAllowRead(allowRead)
                .setAllowWrite(allowWrite)
                .setShardSet(shardSet)
                .setRange(new ShardRange(logicalStart, logicalEnd))
                .setUrl(url)
                ;
    }
}
