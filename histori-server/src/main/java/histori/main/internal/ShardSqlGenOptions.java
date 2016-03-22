package histori.main.internal;

import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.main.BaseMainOptions;
import org.cobbzilla.wizard.server.config.ShardSetConfiguration;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.string.StringUtil.split;

public class ShardSqlGenOptions extends BaseMainOptions {

    public static final String USAGE_SHARD_SET = "Name of the shard sets";
    public static final String OPT_SHARD_SET = "-S";
    public static final String LONGOPT_SHARD_SET= "--shard-sets";
    @Option(name=OPT_SHARD_SET, aliases=LONGOPT_SHARD_SET, usage=USAGE_SHARD_SET, required=true)
    @Getter @Setter private String shardSets;
    public List<String> getShardSetList() { return split(shardSets, ", "); }

    public static final String USAGE_OUTFILE = "Output file for SQL statements. Default is stdout";
    public static final String OPT_OUTFILE = "-o";
    public static final String LONGOPT_OUTFILE= "--output-file";
    @Option(name=OPT_OUTFILE, aliases=LONGOPT_OUTFILE, usage=USAGE_OUTFILE)
    @Getter @Setter private File outfile;
    public boolean hasOutfile () { return outfile != null; }

    public static final String USAGE_SHARD_TABLE = "Name of the DB table containing shard sets. Default is 'shard'";
    public static final String OPT_SHARD_TABLE = "-T";
    public static final String LONGOPT_SHARD_TABLE= "--shard-table";
    @Option(name=OPT_SHARD_TABLE, aliases=LONGOPT_SHARD_TABLE, usage=USAGE_SHARD_TABLE)
    @Getter @Setter private String shardTable = "shard";

    public static final String USAGE_JDBC_URLS = "Set of JDBC urls to use. Use this or a JDBC url-base and a count.";
    public static final String OPT_JDBC_URLS = "-j";
    public static final String LONGOPT_JDBC_URLS= "--jdbc-urls";
    @Option(name=OPT_JDBC_URLS, aliases=LONGOPT_JDBC_URLS, usage=USAGE_JDBC_URLS)
    @Getter @Setter private String jdbcUrls;
    public List<String> getJdbcUrlsList() {
        final List<String> urls;
        if (jdbcUrls != null) {
            urls = split(jdbcUrls, ", ");
        } else {
            if (dbCount == null || dbCount <= 0 || empty(jdbcUrlBase)) die("When "+OPT_JDBC_URLS+"/"+LONGOPT_JDBC_URLS+" is not specified, you must define both options: "+OPT_JDBC_URL_BASE+"/"+LONGOPT_JDBC_URL_BASE+" and "+OPT_DB_COUNT+"/"+LONGOPT_DB_COUNT);
            urls = new ArrayList<>();
            for (int i=0; i<dbCount; i++) {
                urls.add(jdbcUrlBase+i);
            }
        }
        return urls;
    }

    public static final String USAGE_JDBC_URL_BASE = "Base JDBC url to use.";
    public static final String OPT_JDBC_URL_BASE = "-J";
    public static final String LONGOPT_JDBC_URL_BASE= "--jdbc-url-base";
    @Option(name=OPT_JDBC_URL_BASE, aliases=LONGOPT_JDBC_URL_BASE, usage=USAGE_JDBC_URL_BASE)
    @Getter @Setter private String jdbcUrlBase;

    public static final String USAGE_DB_COUNT = "How many databases to work with (use with "+OPT_JDBC_URL_BASE+"/"+LONGOPT_JDBC_URL_BASE+")";
    public static final String OPT_DB_COUNT = "-C";
    public static final String LONGOPT_DB_COUNT= "--db-count";
    @Option(name=OPT_DB_COUNT, aliases=LONGOPT_DB_COUNT, usage=USAGE_DB_COUNT)
    @Setter private Integer dbCount = null;

    public int getDbCount() { return getJdbcUrlsList().size(); }

    public static final String USAGE_DB_MASTER = "JDBC URL of master DB to receive all writes. Must not be included in "+OPT_JDBC_URLS+"/"+LONGOPT_JDBC_URLS;
    public static final String OPT_DB_MASTER = "-M";
    public static final String LONGOPT_DB_MASTER= "--master-db";
    @Option(name=OPT_DB_MASTER, aliases=LONGOPT_DB_MASTER, usage=USAGE_DB_MASTER)
    @Getter @Setter private String dbMaster;
    public boolean hasDbMaster () { return dbMaster != null; }

    public static final String USAGE_MAX_LOGICAL_SHARDS = "Number of logical shards. Default is "+ShardSetConfiguration.DEFAULT_LOGICAL_SHARDS;
    public static final String OPT_MAX_LOGICAL_SHARDS = "-n";
    public static final String LONGOPT_MAX_LOGICAL_SHARDS= "--num-logical-shards";
    @Option(name=OPT_MAX_LOGICAL_SHARDS, aliases=LONGOPT_MAX_LOGICAL_SHARDS, usage=USAGE_MAX_LOGICAL_SHARDS)
    @Getter @Setter private int numLogicalShards = ShardSetConfiguration.DEFAULT_LOGICAL_SHARDS;

}
